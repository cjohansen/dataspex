(ns dataspex.datascript
  (:require #?(:cljs [me.tonsky.persistent-sorted-set :as pss])
            [clojure.core.protocols :as p]
            [datascript.conn]
            [datascript.core :as d]
            [datascript.db]
            [datascript.impl.entity]
            [dataspex.data :as data]
            [dataspex.hiccup :as hiccup]
            [dataspex.protocols :as dp]
            [dataspex.ui :as-alias ui]
            [dataspex.views :as views])
  #?(:clj (:import (me.tonsky.persistent_sorted_set PersistentSortedSet))))

(defn get-datom-entries [[e a v t add?]]
  [{:k e, :v e}
   {:k a, :v a}
   {:path [e a], :v v}
   {:k t :v t}
   {:v add?}])

(defn get-index-entries [index]
  (map-indexed
   (fn [i v]
     {:k i
      :v v})
   index))

(defn get-entities [db]
  (->> (:eavt db)
       (mapv first)
       set
       sort
       (mapv #(d/entity db %))))

(defn attr-sort-val [{:keys [rschema]} a]
  [(if (contains? (:db/unique rschema) a)
     0 1)
   (if (contains? (:db.type/ref rschema) a)
     1 0)
   (if (contains? (:db.cardinality/many rschema) a)
     1 0)
   a])

(defn find-reverse-refs [db e]
  (->> (d/q '[:find ?a ?r
              :in $ ?e
              :where [?r ?a ?e]]
            db (:db/id e))
       (group-by first)
       (sort-by #(attr-sort-val db (first %)))
       (mapv
        (fn [[a es]]
          (let [attr (keyword (namespace a) (str "_" (name a)))]
            {:k attr
             :label attr
             :v (set (mapv #(d/entity db (second %)) es))})))))

(defn get-entity-entries [entity]
  (let [db (d/entity-db entity)]
    (into
     (->> (keys entity)
          (sort-by (partial attr-sort-val db))
          (mapv (fn [k]
                  {:k k
                   :label k
                   :v (get entity k)})))
     (find-reverse-refs db entity))))

(defn summarize-entity [e]
  (let [ks (keys e)]
    (or (when-let [ident (:db/ident e)]
          ident)
        (when-let [uniq (some (:db/unique (:rschema (d/entity-db e))) (keys e))]
          (select-keys e [uniq]))
        (when (< (count ks) 5)
          (into {} e))
        (select-keys e [:db/id]))))

(defn nav-in-db [db p path]
  (data/nav-in
   (if (satisfies? dp/IKeyLookup p)
     (dp/lookup p db)
     (case p
       ::entities (get-entities db)
       (get db p)))
   path))

(defn get-last-tx [db]
  (d/entity db (:max-tx db)))

(defn get-entity-k [coll db-id]
  (if (map? coll)
    (val (first (filterv (comp #{db-id} :db/id key) coll)))
    (first (filterv (comp #{db-id} :db/id) coll))))

(defn get-entities-by-attr [db attr]
  (->> (d/q '[:find [?e ...]
              :in $ ?a
              :where [?e ?a]]
            db attr)
       (mapv #(d/entity db %))
       set))

(defn count-entities-by-attr [db attr]
  (or (d/q '[:find (count ?e) .
             :in $ ?a
             :where [?e ?a]]
           db attr) 0))

(defrecord EntitiesByAttrKey [attr]
  dp/IRenderInline
  (render-inline [_ _]
    (if attr
      [:span.code "Entities by " [::ui/keyword attr]]
      [:span.code "Entities"]))

  dp/IKeyLookup
  (lookup [_ x]
    (if attr
      (get-entities-by-attr (if (d/conn? x) (d/db x) x) attr)
      (get-entities (if (d/conn? x) (d/db x) x)))))

(defn render-entity-index [db opt]
  (let [entities (get-entities db)]
    (if (= 0 (count entities))
      [::ui/code "No entities, better get to it!"]
      (into
       [::ui/enumeration
        [::ui/link
         {::ui/actions [(->> (views/path-to opt [(->EntitiesByAttrKey nil)])
                             (views/navigate-to opt))]}
         (str "All (" (count entities) ")")]]
       (->> db :rschema :db/unique sort
            (mapv (juxt identity #(count-entities-by-attr db %)))
            (remove (comp #{0} second))
            (mapv
             (fn [[attr n]]
               [::ui/clickable
                {::ui/actions [(->> (views/path-to opt [(->EntitiesByAttrKey attr)])
                                    (views/navigate-to opt))]}
                [::ui/keyword attr]
                [::ui/code (str " (" n ")")]])))))))

(defrecord EntityIndex [db]
  dp/IRenderInline
  (render-inline [_ opt]
    (render-entity-index db opt)))

(defn summarize-contents [db]
  (str (count (set (mapv first (:eavt db))))
       " entities, "
       (count (:eavt db))
       " datoms"))

(defn render-conn-inline [conn]
  [::ui/code (str "#datascript/Conn [" (summarize-contents (d/db conn)) "]")])

(defn render-database-inline [db]
  [::ui/code (str "#datascript/DB [" (summarize-contents db) "]")])

(defn navigate-to [opt xs]
  (->> (views/path-to opt xs)
       (views/navigate-to opt)))

(defn render-datom [[e a v t add?] opt & [{:keys [alias]}]]
  [(or alias ::ui/inline-tuple) {::ui/prefix "datom"}
   [::ui/number {::ui/actions [(navigate-to opt [e])]} e]
   [::ui/keyword {::ui/actions [(navigate-to opt [a])]} a]
   (-> (hiccup/render-inline v)
       (hiccup/add-attr ::ui/actions [(navigate-to opt [e a])]))
   [::ui/number {::ui/actions [(navigate-to opt [t])]} t]
   [::ui/boolean add?]])

(defn render-database-dictionary [db opt]
  (hiccup/render-entries-dictionary
   db
   (into [{:k :schema
           :label 'Schema
           :v (:schema db)}
          {:label 'Entities
           :v (->EntityIndex db)}
          {:k :eavt
           :label (hiccup/string-label "Datoms by entity (eavt)")
           :v (:eavt db)}]
         (->> [:aevt :avet :max-eid :max-tx :rschema :hash]
              (mapv (fn [k] {:k k, :label k, :v (k db)}))))
   opt))

(defn render-index-dictionary [index opt]
  (into [::ui/dictionary {:class :table-auto}]
        (mapv #(render-datom % opt {:alias ::ui/tuple}) index)))

(defn render-db-source [db opt]
  (dp/render-dictionary (:eavt db) opt))

(extend-type datascript.conn.Conn
  dp/INavigatable
  (nav-in [conn [p & path]]
    (nav-in-db (d/db conn) p path))

  dp/IRenderInline
  (render-inline [conn _]
    (render-conn-inline conn))

  dp/IRenderDictionary
  (render-dictionary [conn opt]
    (render-database-dictionary (d/db conn) opt))

  dp/IRenderSource
  (render-source [conn opt]
    (render-db-source (d/db conn) opt)))

(extend-type datascript.db.DB
  dp/INavigatable
  (nav-in [db [p & path]]
    (nav-in-db db p path))

  dp/IDiffable
  (->diffable [db]
    (into [] (:eavt db)))

  dp/IAuditable
  (get-audit-summary [self]
    (:dataspex.audit/summary (get-last-tx self)))

  (get-audit-details [self]
    (:dataspex.audit/details (get-last-tx self)))

  dp/IRenderInline
  (render-inline [db _]
    (render-database-inline db))

  dp/IRenderDictionary
  (render-dictionary [db opt]
    (render-database-dictionary db opt))

  dp/IRenderSource
  (render-source [db opt]
    (render-db-source db opt)))

(extend-type datascript.db.Datom
  dp/IRenderInline
  (render-inline [datom opt]
    (render-datom datom opt))

  dp/IRenderDictionary
  (render-dictionary [datom opt]
    (hiccup/render-entries-dictionary datom (get-datom-entries datom) opt)))

(extend-type #?(:clj PersistentSortedSet
                :cljs pss/BTSet)
  dp/IRenderInline
  (render-inline [pss opt]
    (hiccup/render-inline-set pss opt))

  dp/IRenderDictionary
  (render-dictionary [pss opt]
    (if (instance? datascript.db.Datom (first pss))
      (render-index-dictionary pss opt)
      (hiccup/render-entries-dictionary pss (get-index-entries pss) opt))))

(defrecord EntityKey [id summary]
  p/Datafiable
  (datafy [_]
    summary)

  dp/IKeyLookup
  (lookup [_ coll]
    (get-entity-k coll id)))

(defn make-entity-key [e]
  (EntityKey. (:db/id e) (summarize-entity e)))

(extend-type datascript.impl.entity.Entity
  dp/IKey
  (to-key [e]
    (make-entity-key e))

  dp/IRenderInline
  (render-inline [entity opt]
    (hiccup/render-inline (summarize-entity entity) opt))

  dp/IRenderDictionary
  (render-dictionary [entity opt]
    (hiccup/render-entries-dictionary entity (get-entity-entries entity) opt)))
