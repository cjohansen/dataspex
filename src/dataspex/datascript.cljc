(ns dataspex.datascript
  (:require #?(:cljs [me.tonsky.persistent-sorted-set :as pss])
            [datascript.conn]
            [datascript.core :as d]
            [datascript.db]
            [datascript.impl.entity]
            [dataspex.datalog :as datalog]
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
  (mapv (fn [v] {:v v}) index))

(defn get-entities [db]
  (->> (:eavt db)
       (mapv first)
       set
       sort
       (mapv #(d/entity db %))
       set))

(defn get-entities-by-attr [db attr]
  (->> (d/q '[:find [?e ...]
              :in $ ?a
              :where [?e ?a]]
            db attr)
       (sort)
       (map #(d/entity db %))))

(defn get-last-tx [db]
  (d/entity db (:max-tx db)))

(defn count-entities-by-attr [db attr]
  (or (d/q '[:find (count ?e) .
             :in $ ?a
             :where [?e ?a]]
           db
           attr) 0))

(defn summarize-contents [db]
  (str (count (set (mapv first (:eavt db))))
       " entities, "
       (count (:eavt db))
       " datoms"))

(defn render-conn-inline [conn]
  [::ui/code (str "#datascript/Conn [" (summarize-contents (d/db conn)) "]")])

(defn render-database-inline [db]
  [::ui/code (str "#datascript/DB [" (summarize-contents db) "]")])

(defrecord Schema [db]
  dp/IKeyLookup
  (dp/lookup [_ db]
    (:schema db))

  dp/IRenderInline
  (render-inline [_ _]
    [::ui/symbol "Schema"]))

(defn render-database-dictionary [db opt]
  (->> [:aevt :avet :max-eid :max-tx :rschema :hash]
       (mapv (fn [k] {:k k, :label k, :v (k db)}))
       (into [{:k (->Schema db)
               :label 'Schema
               :v (:schema db)}
              {:label 'Entities
               :v (datalog/->EntityIndex db)}
              {:k :eavt
               :label (hiccup/string-label "Datoms by entity (eavt)")
               :v (:eavt db)}])
       (hiccup/render-entries-dictionary db opt)))

(defn render-index-dictionary [index opt]
  (into [::ui/dictionary {:class :table-auto}]
        (mapv #(datalog/render-datom % opt {:alias ::ui/tuple}) index)))

(defn render-db-source [db opt]
  (dp/render-dictionary (:eavt db) opt))

(extend-type dataspex.datalog.Attr
  datalog/IDatabaseLookup
  (lookup-in-db [attr db]
    (get-in db [:schema (:a attr)])))

(extend-type dataspex.datalog.AttrValue
  datalog/IDatabaseLookup
  (lookup-in-db [attr-val db]
    (let [{:keys [a v]} attr-val]
      (if (and (number? v) (-> db :rschema :db.type/ref a))
        (d/entity db v)
        v))))

(extend-type datascript.conn.Conn
  dp/INavigatable
  (nav-in [conn path]
    (datalog/nav-in-db (d/db conn) path))

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
  datalog/Database
  (count-entities-by-attr [db attr]
    (count-entities-by-attr db attr))

  (entity [db entity-ref]
    (d/entity db entity-ref))

  (get-entities [db]
    (get-entities db))

  (get-entities-by-attr [db attr]
    (get-entities-by-attr db attr))

  (get-attr-sort-val [{:keys [rschema]} a]
    [(if (contains? (:db/unique rschema) a)
       0 1)
     (if (contains? (:db.type/ref rschema) a)
       1 0)
     (if (contains? (:db.cardinality/many rschema) a)
       1 0)
     a])

  (get-unique-attrs [db]
    (-> db :rschema :db/unique))

  (q [db query args]
    (apply d/q query db args))

  dp/INavigatable
  (nav-in [db path]
    (datalog/nav-in-db db path))

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
    (datalog/render-datom datom opt))

  dp/IRenderDictionary
  (render-dictionary [datom opt]
    (hiccup/render-entries-dictionary datom opt (get-datom-entries datom))))

(extend-type #?(:clj PersistentSortedSet
                :cljs pss/BTSet)
  dp/IRenderInline
  (render-inline [pss opt]
    (hiccup/render-inline-set pss opt))

  dp/IRenderDictionary
  (render-dictionary [pss opt]
    (if (instance? datascript.db.Datom (first pss))
      (render-index-dictionary pss opt)
      (hiccup/render-entries-dictionary pss opt (get-index-entries pss)))))

(extend-type datascript.impl.entity.Entity
  datalog/Entity
  (entity-db [entity]
    (d/entity-db entity))

  (get-ref-attrs [e]
    (->> (keys e)
         (filterv (:db/unique (:rschema (d/entity-db e))))
         not-empty))

  (get-primitive-attrs [e]
    (let [rschema (:rschema (d/entity-db e))
          unwanted (into (:db.type/ref rschema) (:db.cardinality/many rschema))]
      (->> (keys e)
           (remove unwanted))))

  (get-reverse-ref-attrs [entity]
    (let [db (d/entity-db entity)]
      (d/q '[:find [?a ...]
             :in $ [?a ...] ?e
             :where [?r ?a ?e]]
           db (-> db :rschema :db.type/ref) (:db/id entity))))

  dp/IKey
  (to-key [e]
    (datalog/make-entity-key e))

  dp/IRenderInline
  (render-inline [entity opt]
    (datalog/render-inline-entity entity opt))

  dp/IRenderDictionary
  (render-dictionary [entity opt]
    (hiccup/render-entries-dictionary entity opt (datalog/get-entity-entries entity))))

(comment

  (def conn (d/create-conn {:person/id {:db/unique :db.unique/identity}
                            :person/friends {:db/valueType :db.type/ref
                                             :db/cardinality :db.cardinality/many}}))

  (d/transact! conn [{:person/id "bob"
                      :person/name "Bob"
                      :person/friends ["alice" "wendy"]}
                     {:db/id "alice"
                      :person/id "alice"
                      :person/name "Alice"}
                     {:db/id "wendy"
                      :person/id "wendy"
                      :person/name "Wendy"}])

  (def db (d/db conn))

  (datalog/get-primitive-attrs bob)
  (def bob (datalog/entity db [:person/id "bob"]))
  (def alice (datalog/entity db [:person/id "alice"]))

  (satisfies? dp/IKeyLookup (datalog/make-entity-key bob))
  (hash (datalog/make-entity-key bob))
  (hash (datalog/make-entity-key bob))

  (datalog/get-entity-entries bob)
  (datalog/get-reverse-ref-attrs alice)

  (datalog/find-reverse-refs db alice)
)
