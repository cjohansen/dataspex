(ns dataspex.datalog
  (:require [clojure.core.protocols :as p]
            [dataspex.data :as data]
            [dataspex.hiccup :as hiccup]
            [dataspex.protocols :as dp]
            [dataspex.ui :as-alias ui]
            [dataspex.views :as views]))

(defprotocol Entity
  (entity-db [entity])
  (get-ref-attrs [entity])
  (get-primitive-attrs [entity])
  (get-reverse-ref-attrs [entity]))

(defprotocol Database
  (count-entities-by-attr [db attr])
  (entity [db entity-ref])
  (get-entities [db])
  (get-entities-by-attr [db attr])
  (get-unique-attrs [db])
  (get-attr-sort-val [db a])
  (get-attrs-used-with [db a]))

(defprotocol IDatabaseLookup
  (lookup-in-db [x db]))

(declare render-entity-index)

(defn ->entity-entry [entity]
  {:k (data/as-key entity)
   :label (:db/id entity)
   :v entity})

(defrecord EntityIndex [db]
  dp/IRenderInline
  (render-inline [_ opt]
    (render-entity-index db opt))

  dp/IRenderDictionary
  (render-dictionary [_ opt]
    (->> (get-entities db)
         (map ->entity-entry)
         (hiccup/render-entries-dictionary db opt))))

(deftype EntitiesByAttrIndex [db attr]
  p/Datafiable
  (datafy [_]
    (get-entities-by-attr db attr))

  #?(:clj  clojure.lang.Counted
     :cljs ICounted)
  (#?(:clj count :cljs -count) [_]
    (count (get-entities-by-attr db attr)))

  dp/IRenderDictionary
  (render-dictionary [_ opt]
    (->> (get-entities-by-attr db attr)
         (map ->entity-entry)
         (hiccup/render-entries-dictionary db opt)))

  dp/IRenderTable
  (tableable? [_ _]
    true)

  (render-table [_ opt]
    (hiccup/render-map-table
     (get-entities-by-attr db attr)
     (->> (get-attrs-used-with db attr)
          (sort-by #(get-attr-sort-val db %)))
     opt)))

(defrecord EntitiesByAttrKey [attr]
  dp/IRenderInline
  (render-inline [_ _]
    (if attr
      [:span.code "Entities by " [::ui/keyword attr]]
      [:span.code "Entities"]))

  dp/IKeyLookup
  (lookup [_ db]
    (if attr
      (->EntitiesByAttrIndex db attr)
      (->EntityIndex db))))

(defn nav-in-db [db path]
  (loop [[p & ps] (reverse path)
         rest-path ()]
    (if (nil? p)
      (let [[p & ps] path]
        (data/nav-in
         (if (satisfies? dp/IKeyLookup p)
           (dp/lookup p db)
           (get db p))
         ps))
      (if (satisfies? IDatabaseLookup p)
        (data/nav-in (lookup-in-db p db) rest-path)
        (recur ps (conj rest-path p))))))

(defn find-reverse-refs [db entity]
  (->> (get-reverse-ref-attrs entity)
       (sort-by #(get-attr-sort-val db (first %)))
       (mapv
        (fn [a]
          (let [attr (keyword (namespace a) (str "_" (name a)))]
            {:k attr
             :label attr
             :v (set (get entity attr))})))))

(defn get-entity-entries [entity]
  (let [db (entity-db entity)]
    (into
     (->> (keys entity)
          (sort-by (partial get-attr-sort-val db))
          (mapv (fn [k]
                  {:k k
                   :label k
                   :v (get entity k)})))
     (find-reverse-refs db entity))))

(defn get-entity-k [coll db-id]
  (if (map? coll)
    (val (first (filterv (comp #{db-id} :db/id key) coll)))
    (first (filterv (comp #{db-id} :db/id) coll))))

(defn summarize-entity [e]
  (let [ks (keys e)]
    (or (when-let [ident (:db/ident e)]
          ident)
        (when-let [uniq (seq (get-ref-attrs e))]
          (select-keys e uniq))
        (when (< (count ks) 5)
          (into {} e))
        (select-keys e [:db/id]))))

(defrecord EntityId [id]
  p/Datafiable
  (datafy [_]
    {:db/id id})

  IDatabaseLookup
  (lookup-in-db [_ db]
    (entity db id)))

(defrecord Attr [a]
  p/Datafiable
  (datafy [_]
    a))

(defrecord AttrValue [a v]
  p/Datafiable
  (datafy [_]
    v))

(defrecord EntityKey [id summary]
  p/Datafiable
  (datafy [_]
    summary)

  dp/IKeyLookup
  (lookup [_ coll]
    (get-entity-k coll id))

  IDatabaseLookup
  (lookup-in-db [_ db]
    (entity db id)))

(defn make-entity-key [e]
  (EntityKey. (:db/id e) (summarize-entity e)))

(deftype DatomKey [datom]
  p/Datafiable
  (datafy [_]
    (:e datom))

  dp/IKeyLookup
  (lookup [_ _]
    datom))

(defn make-datom-key [datom]
  (->DatomKey datom))

(defn navigate-to [opt xs]
  (->> (views/path-to opt xs)
       (views/navigate-to opt)))

(defn render-datom [[e a v t add?] opt & [{:keys [alias]}]]
  (let [e-k (->EntityId e)
        a-k (->Attr a)]
    [(or alias ::ui/inline-tuple) {::ui/prefix "datom"}
     [::ui/number {::ui/actions (navigate-to opt [e-k])} e]
     [::ui/keyword {::ui/actions (navigate-to opt [a-k])} a]
     (-> (hiccup/render-inline v)
         (hiccup/add-attr ::ui/actions (navigate-to opt [e-k a-k (->AttrValue a v)])))
     [::ui/number {::ui/actions (navigate-to opt [(->EntityId t)])} t]
     [::ui/boolean add?]]))

(defn render-inline-entity [entity opt]
  (let [entity-m (select-keys entity (get-primitive-attrs entity))
        entity-m (if (empty? entity-m) (into {} entity) entity-m)]
    (if (hiccup/summarize? entity-m opt)
      (hiccup/render-inline (summarize-entity entity) opt)
      (hiccup/render-inline entity-m opt))))

(defn render-entity-index [db opt]
  (let [entities (get-entities db)
        n (bounded-count (inc views/max-items) entities)]
    (if (= 0 n)
      [::ui/code "No entities, better get to it!"]
      (into
       [::ui/ul
        [::ui/link
         {::ui/actions (->> (views/path-to opt [(->EntitiesByAttrKey nil)])
                            (views/navigate-to opt))}
         (if (< views/max-items n)
           (str "All (" views/max-items "+)")
           (str "All (" n ")"))]]
       (->> (get-unique-attrs db)
            (sort)
            (mapv (juxt identity #(count-entities-by-attr db %)))
            (remove (comp #{0} second))
            (mapv
             (fn [[attr n]]
               [::ui/clickable
                {::ui/actions (->> (views/path-to opt [(->EntitiesByAttrKey attr)])
                                   (views/navigate-to opt))}
                [::ui/keyword attr]
                [::ui/code (str " (" n ")")]])))))))
