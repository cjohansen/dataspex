(ns dataspex.datahike
  (:require [clojure.core.protocols :as p]
            [datahike.api :as d]
            [dataspex.data :as data]
            [dataspex.datalog :as datalog]
            [dataspex.hiccup :as hiccup]
            [dataspex.protocols :as dp]
            [dataspex.ui :as-alias ui]
            [dataspex.views :as views]))

(defn attr-sort-val [attribute]
  [(namespace (:db/ident attribute))
   (if (:db/unique attribute)
     0 1)
   (if (= :db.type/ref (:db/valueType attribute))
     1 0)
   (if (= :db.cardinality/many (:db/cardinality attribute))
     1 0)
   (name (:db/ident attribute))])

(defn load-schema [db]
  (->> (vals (d/schema db))
       (map #(dissoc % :db/id))
       (sort-by attr-sort-val)))

(defn render-schema-dictionary [db opt]
  (let [schema (load-schema db)]
    (->> (mapv (fn [attr]
                 {:k (:db/ident attr)
                  :label (:db/ident attr)
                  :v attr})
               schema)
         (hiccup/render-entries-dictionary schema opt))))

(defrecord Schema [db]
  p/Datafiable
  (datafy [_]
    (load-schema db))

  dp/INavigatable
  (nav-in [_ [attr & path]]
    (data/nav-in (into {} (d/entity db attr)) path))

  dp/IRenderDictionary
  (render-dictionary [_ opt]
    (render-schema-dictionary db opt)))

(defrecord SchemaKey [db]
  dp/IKeyLookup
  (dp/lookup [_ _path]
    (->Schema db))

  dp/IRenderInline
  (render-inline [_ _]
    [::ui/symbol "Schema"]))

(defn summarize-contents [db]
  ;; TODO: This probably won't fly on a large database
  (let [datoms (d/seek-datoms db :eavt)]
    (str (count (distinct (map :e datoms)))
         " entities, "
         (count datoms)
         " datoms")))

(defn render-conn-inline [conn]
  [::ui/code (str "#datahike.connector.Connection [" (summarize-contents (d/db conn)) "]")])

(defn render-db-inline [db]
  [::ui/code (str "#datahike.db.Db [" (summarize-contents db) "]")])

(defrecord TxIndex [db]
  dp/IRenderInline
  (render-inline [_ _]
    (let [n (bounded-count (inc views/max-items) (datalog/get-entities-by-attr db :db/txInstant))]
      [::ui/code
       (if (< views/max-items n)
         (str views/max-items "+ transactions")
         (str n " transactions"))]))

  dp/IRenderDictionary
  (render-dictionary [_ opt]
    (->> (datalog/get-entities-by-attr db :db/txInstant)
         (map datalog/->entity-entry)
         (hiccup/render-entries-dictionary db opt))))

(defrecord TransactionsKey [db]
  dp/IKeyLookup
  (dp/lookup [_ db]
    (->TxIndex db))

  dp/IRenderInline
  (render-inline [_ _]
    [::ui/symbol "Transactions"]))

(defn render-database-dictionary [db opt]
  (->> [{:k (->SchemaKey db)
         :label 'Schema
         :v (mapv :db/ident (load-schema db))
         :copyable? false}
        {:label 'Entities
         :v (datalog/->EntityIndex db)
         :copyable? false}
        {:label 'Transactions
         :v (->TxIndex db)
         :k (->TransactionsKey db)
         :copyable? false}]
       (hiccup/render-entries-dictionary db opt)))

(extend-type datahike.connector.Connection
  dp/INavigatable
  (nav-in [conn path]
    (datalog/nav-in-db (d/db conn) path))

  dp/IRenderInline
  (render-inline [conn _]
    (render-conn-inline conn))

  dp/IRenderDictionary
  (render-dictionary [conn opt]
    (render-database-dictionary (d/db conn) opt)))

(defn get-entities-by-attr [db attr]
  (->> (d/datoms db :aevt attr)
       (map :e)
       distinct
       (map #(d/entity db %))))

(defn get-attrs-used-with [db attr]
  (d/q '[:find [?attr ...]
         :in $ ?used-a
         :where
         [?e ?used-a]
         [?e ?attr]]
       db attr))

(defn -get-attr-sort-val [db a]
  (if (= :db/id a)
    [0]
    (attr-sort-val (or (d/entity db a) {:db/ident a}))))

(extend-type datahike.db.DB
  datalog/Database
  (count-entities-by-attr [db attr]
    (count (seq (d/datoms db :avet attr))))

  (entity [db entity-ref]
    (d/entity db entity-ref))

  (get-entities [db]
    ;; TODO: Find an efficient way to do this
    (->> (d/seek-datoms db :eavt)
         (map :e)
         distinct
         (map #(d/entity db %))
         (remove :db/valueType)
         (remove :db/txInstant)))

  (get-entities-by-attr [db attr]
    (get-entities-by-attr db attr))

  (get-attrs-used-with [db attr]
    (get-attrs-used-with db attr))

  (get-attr-sort-val [db a]
    (-get-attr-sort-val db a))

  (get-unique-attrs [db]
    (->> (d/q '[:find [?attr ...]
                :where
                [?a :db/unique]
                [?a :db/ident ?attr]]
              db)
         sort))

  dp/INavigatable
  (nav-in [db path]
    (datalog/nav-in-db db path))

  dp/IRenderInline
  (render-inline [db _]
    (render-db-inline db))

  dp/IRenderDictionary
  (render-dictionary [db opt]
    (render-database-dictionary db opt)))

(extend-type dataspex.datalog.Attr
  datalog/IDatabaseLookup
  (lookup-in-db [attr db]
    (into {} (d/entity db (:a attr)))))

(extend-type dataspex.datalog.AttrValue
  datalog/IDatabaseLookup
  (lookup-in-db [attr-val db]
    (if (= :db.type/ref (:db/valueType (d/entity db (:a attr-val))))
      (d/entity db (:v attr-val))
      (:v attr-val))))

(extend-type datahike.datom.Datom
  dp/IRenderInline
  (render-inline [datom opt]
    (datalog/render-datom datom opt)))

(defn get-unique-entity-attrs [e]
  (d/q '[:find [?a ...]
         :in $ [?a ...]
         :where
         [?attr :db/ident ?a]
         [?attr :db/unique]]
       (d/entity-db e) (keys e)))

(defn get-primitive-attrs [e]
  (d/q '[:find [?a ...]
         :in $ [?a ...]
         :where
         [?attr :db/ident ?a]
         (not [?attr :db/cardinality :db.cardinality/many])]
       (d/entity-db e) (keys e)))

(defn get-reverse-ref-attrs [entity]
  (d/q '[:find [?attr ...]
         :in $ ?e
         :where
         [?a :db/valueType :db.type/ref]
         [?a :db/ident ?attr]
         [?r ?attr ?e]]
       (d/entity-db entity)
       (:db/id entity)))

(defn render-entity-dictionary [entity opt]
  (hiccup/render-entries-dictionary entity opt (datalog/get-entity-entries entity)))

(extend-type datahike.impl.entity.Entity
  datalog/Entity
  (entity-db [entity]
    (d/entity-db entity))

  (get-unique-entity-attrs [e]
    (get-unique-entity-attrs e))

  (get-primitive-attrs [e]
    (get-primitive-attrs e))

  (get-reverse-ref-attrs [e]
    (get-reverse-ref-attrs e))

  dp/IKey
  (to-key [e]
    (datalog/make-entity-key e))

  dp/IRenderInline
  (render-inline [entity opt]
    (datalog/render-inline-entity entity opt))

  dp/IRenderDictionary
  (render-dictionary [entity opt]
    (render-entity-dictionary entity opt)))
