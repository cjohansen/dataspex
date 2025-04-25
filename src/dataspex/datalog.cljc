(ns dataspex.datalog
  (:require [clojure.core.protocols :as p]
            [dataspex.protocols :as dp]
            [dataspex.hiccup :as hiccup]
            [dataspex.data :as data]))

(defprotocol Entity
  (entity-db [entity])
  (get-ref-attrs [entity])
  (get-primitive-attrs [entity])
  (get-reverse-ref-attrs [entity]))

(defprotocol Database
  (entity [db entity-ref])
  (get-attr-sort-val [db a]))

(defprotocol IDatabaseLookup
  (lookup-in-db [x db]))

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
        (when-let [uniq (get-ref-attrs e)]
          (select-keys e uniq))
        (when (< (count ks) 5)
          (into {} e))
        (select-keys e [:db/id]))))

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

(defn render-inline-entity [entity opt]
  (let [entity-m (select-keys entity (get-primitive-attrs entity))]
    (if (hiccup/summarize? entity-m opt)
      (hiccup/render-inline (summarize-entity entity) opt)
      (hiccup/render-inline entity-m opt))))
