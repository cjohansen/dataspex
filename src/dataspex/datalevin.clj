(ns dataspex.datalevin
  (:require [clojure.core.protocols :as p]
            [clojure.string :as str]
            [datalevin.core :as d]
            [datalevin.db :as db]
            [dataspex.data :as data]
            [dataspex.datalog :as datalog]
            [dataspex.hiccup :as hiccup]
            [dataspex.protocols :as dp]
            [dataspex.ui :as-alias ui]))

(defn get-entities [db]
  (->> (d/q '[:find [?e ...]
              :where [?e]]
            db)
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

(defn get-attrs-used-with [db attr]
  (d/q '[:find [?attr ...]
         :in $ ?a
         :where
         [?e ?a]
         [?e ?attr]]
       db attr))

(defn get-last-tx [db]
  (d/entity db (:max-tx db)))

(defn count-entities-by-attr [db attr]
  (or (d/q '[:find (count ?e) .
             :in $ ?a
             :where [?e ?a]]
           db
           attr) 0))

(defn summarize-contents [db]
  (str (d/q '[:find (count ?e) .
              :where [?e]]
            db)
       " entities"))

(defn render-conn-inline [conn]
  [::ui/code (str "#datalevin/Conn [" (summarize-contents (d/db conn)) "]")])

(defn render-database-inline [db]
  [::ui/code (str "#datalevin/DB [" (summarize-contents db) "]")])

(defrecord Schema [db]
  p/Datafiable
  (datafy [_]
    (db/-schema db))

  dp/IKeyLookup
  (dp/lookup [_ db]
    (db/-schema db))

  dp/IRenderInline
  (render-inline [_ _]
    [::ui/symbol "Schema"]))

(defrecord SchemaKey [db]
  dp/IKeyLookup
  (dp/lookup [_ db]
    (->Schema db))

  dp/IRenderInline
  (render-inline [_ _]
    [::ui/symbol "Schema"]))

(defn render-database-dictionary [db opt]
  (->> [{:k (->SchemaKey db)
         :label 'Schema
         :v (keys (db/-schema db))
         :copyable? false}
        {:label 'Entities
         :v (datalog/->EntityIndex db)
         :copyable? false}]
       (hiccup/render-entries-dictionary db opt)))

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
      (if (and (number? v) (= :db.type/ref (get-in (db/-schema db) [a :db/valueType])))
        (d/entity db v)
        v))))

(defrecord Conn [conn]
  dp/INavigatable
  (nav-in [_ path]
    (datalog/nav-in-db (d/db conn) path))

  dp/IRenderInline
  (render-inline [_ _]
    (render-conn-inline conn))

  dp/IRenderDictionary
  (render-dictionary [_ opt]
    (render-database-dictionary (d/db conn) opt))

  dp/IRenderSource
  (render-source [_ opt]
    (render-db-source (d/db conn) opt))

  dp/Watchable
  (get-val [ref]
    @ref)

  (watch [ref k f]
    (add-watch ref k (fn [_ _ old-data new-data] (f old-data new-data nil)))
    k)

  (unwatch [ref k]
    (remove-watch ref k))

  dp/INavigatable
  (nav-in [_ path]
    (datalog/nav-in-db (d/db conn) path)))

(defn reify-conn [x]
  (when (and (data/derefable? x)
             (some-> x deref :store type str (str/includes? "datalevin")))
    (->Conn x)))

(defn nav-in-db [db path]
  (datalog/nav-in-db db path))

(defn -get-unique-attrs [db]
  (->> (db/-schema db)
       (filterv (comp :db/unique second))
       (mapv first)
       set))

(defn -get-attr-sort-val [db a]
  (let [attr (get (db/-schema db) a)]
    [(if (= :db/id a) 0 1)
     (namespace a)
     (if (:db/unique attr)
       0 1)
     (if (= :db.type/ref (:db/valueType attr))
       1 0)
     (if (= :db.cardinality/many (:db/cardinality attr))
       1 0)
     a]))

(extend-type datalevin.db.DB
  datalog/Database
  (count-entities-by-attr [db attr]
    (count-entities-by-attr db attr))

  (entity [db entity-ref]
    (d/entity db entity-ref))

  (get-entities [db]
    (get-entities db))

  (get-entities-by-attr [db attr]
    (get-entities-by-attr db attr))

  (get-attr-sort-val [db a]
    (-get-attr-sort-val db a))

  (get-attrs-used-with [db attr]
    (get-attrs-used-with db attr))

  (get-unique-attrs [db]
    (-get-unique-attrs db))

  dp/INavigatable
  (nav-in [db path]
    (nav-in-db db path))

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

(defn get-schema-ref-attrs [db]
  (->> (db/-schema db)
       (filterv (comp #{:db.type/ref} :db/valueType second))
       (mapv first)
       set))

(defn get-schema-unique-attrs [db]
  (->> (db/-schema db)
       (filterv (comp :db/unique second))
       (mapv first)
       set))

(defn -get-unique-entity-attrs [e]
  (->> (keys e)
       (filterv (get-schema-unique-attrs (d/entity-db e)))
       not-empty))

(defn -get-primitive-attrs [e]
  (->> (keys e)
       (filterv (->> (db/-schema (d/entity-db e))
                     (remove (comp #{:db.type/ref} :db/valueType second))
                     (remove (comp #{:db.cardinality/many} :db/cardinality second))
                     (mapv first)
                     set))
       not-empty))

(defn -get-reverse-ref-attrs [entity]
  (let [db (d/entity-db entity)]
    (d/q '[:find [?a ...]
           :in $ [?a ...] ?e
           :where [?r ?a ?e]]
         db
         (get-schema-ref-attrs db)
         (:db/id entity))))

(extend-type datalevin.entity.Entity
  datalog/Entity
  (entity-db [entity]
    (d/entity-db entity))

  (get-unique-entity-attrs [e]
    (-get-unique-entity-attrs e))

  (get-primitive-attrs [e]
    (-get-primitive-attrs e))

  (get-reverse-ref-attrs [entity]
    (-get-reverse-ref-attrs entity))

  dp/IKey
  (to-key [e]
    (datalog/make-entity-key e))

  ;; Datalevin provides a datafy implementation for entites that doesn't include
  ;; underscore/reverse ref attributes, so these entities must take control of
  ;; navigation for backwards refs to work.
  dp/INavigatable
  (nav-in [e ks]
    (let [[k & ks*] ks]
      (cond-> (get e k)
        (seq ks*) (data/nav-in ks*))))

  dp/IRenderInline
  (render-inline [entity opt]
    (datalog/render-inline-entity entity opt))

  dp/IRenderDictionary
  (render-dictionary [entity opt]
    (hiccup/render-entries-dictionary entity opt (datalog/get-entity-entries entity))))
