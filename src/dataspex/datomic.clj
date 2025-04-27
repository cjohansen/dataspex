(ns dataspex.datomic
  (:require [clojure.core.protocols :as p]
            [dataspex.data :as data]
            [dataspex.datalog :as datalog]
            [dataspex.hiccup :as hiccup]
            [dataspex.protocols :as dp]
            [dataspex.ui :as-alias ui]
            [dataspex.views :as views]
            [datomic.api :as d]))

(defn attr-sort-val [attribute]
  [(namespace (:db/ident attribute))
   (if (:db/unique attribute)
     0 1)
   (if (= :db.type/ref (:db/valueType attribute))
     1 0)
   (if (= :db.cardinality/many (:db/cardinality attribute))
     1 0)
   (name (:db/ident attribute))])

(def builtin-attrs
  #{:db.alter/attribute
    :db.attr/preds
    :db.entity/attrs
    :db.entity/preds
    :db.excise/attrs
    :db.excise/before
    :db.excise/beforeT
    :db.install/attribute
    :db.install/function
    :db.install/partition
    :db.install/valueType
    :db.sys/partiallyIndexed
    :db.sys/reId
    :db/cardinality
    :db/code
    :db/doc
    :db/ensure
    :db/excise
    :db/fn
    :db/fulltext
    :db/ident
    :db/index
    :db/isComponent
    :db/lang
    :db/noHistory
    :db/system-tx
    :db/tupleAttrs
    :db/tupleType
    :db/tupleTypes
    :db/txInstant
    :db/unique
    :db/valueType
    :dte/valueType ;; datomic-type-extensions
    :fressian/tag})

(defn load-schema [db]
  (->> (d/q '[:find [?attr ...]
              :where [?attr :db/valueType]]
            db)
       (mapv #(d/entity db %))
       (remove (comp builtin-attrs :db/ident))
       (sort-by attr-sort-val)
       (mapv #(into {} %))))

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
  (dp/lookup [_ db]
    (->Schema db))

  dp/IRenderInline
  (render-inline [_ _]
    [::ui/symbol "Schema"]))

(defn render-conn-inline [conn]
  [::ui/code (str "#datomic.Connection [" (:datoms (d/db-stats (d/db conn))) " datoms]")])

(defn render-db-inline [db]
  [::ui/code (str "#datomic.db.Db [" (:datoms (d/db-stats db)) " datoms]")])

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
         :v (mapv :db/ident (load-schema db))}
        {:label 'Entities
         :v (datalog/->EntityIndex db)}
        {:label 'Transactions
         :v (->TxIndex db)
         :k (->TransactionsKey db)}]
       (hiccup/render-entries-dictionary db opt)))

(defn tx-data->diff [tx-data]
  (mapv (fn [datom]
          [[(:e datom)] :+ datom]) tx-data))

(extend-type datomic.Connection
  dp/INavigatable
  (nav-in [conn path]
    (datalog/nav-in-db (d/db conn) path))

  dp/IRenderInline
  (render-inline [conn _]
    (render-conn-inline conn))

  dp/IRenderDictionary
  (render-dictionary [conn opt]
    (render-database-dictionary (d/db conn) opt))

  dp/Watchable
  (get-val [conn]
    (d/db conn))

  (watch [conn _ f]
    (let [queue (d/tx-report-queue conn)
          watching? (volatile! true)]
      (future
        (while @watching?
          (let [{:keys [db-before db-after tx-data]} (.take queue)]
            (f db-before db-after (tx-data->diff tx-data)))))
      watching?))

  (unwatch [_ subscription]
    (vreset! subscription false)))

(defn get-entities-by-attr [db attr]
  (->> (d/datoms db :aevt attr)
       (map :e)
       distinct
       (map #(d/entity db %))))

(extend-type datomic.db.Db
  datalog/Database
  (count-entities-by-attr [db attr]
    (let [attr (if (number? attr)
                 (:db/ident (d/entity db attr))
                 attr)]
      (or (get-in (d/db-stats db) [:attrs attr :count]) 0)))

  (entity [db entity-ref]
    (d/entity db entity-ref))

  (get-entities [db]
    (->> (d/datoms db :eavt)
         (map :e)
         distinct
         (map #(d/entity db %))
         (remove :db/txInstant)
         (remove #(< (:db/id %) 1000))))

  (get-entities-by-attr [db attr]
    (get-entities-by-attr db attr))

  (get-attr-sort-val [db a]
    (attr-sort-val (d/entity db a)))

  (get-unique-attrs [db]
    (->> (d/q '[:find [?attr ...]
                :where
                [?a :db/unique]
                [?a :db/ident ?attr]]
              db)
         (remove builtin-attrs)
         sort))

  (q [db query args]
    (apply d/q query db args))

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

(extend-type datomic.db.Datum
  dp/IRenderInline
  (render-inline [datom opt]
    (datalog/render-datom datom opt)))

(extend-type datomic.query.EntityMap
  datalog/Entity
  (entity-db [entity]
    (d/entity-db entity))

  (get-ref-attrs [e]
    (d/q '[:find [?a ...]
           :in $ [?a ...]
           :where
           [?attr :db/ident ?a]
           [?attr :db/unique]]
         (d/entity-db e) (keys e)))

  (get-primitive-attrs [e]
    (d/q '[:find [?a ...]
           :in $ [?a ...]
           :where
           [?attr :db/ident ?a]
           (not [?attr :db/cardinality :db.cardinality/many])]
         (d/entity-db e) (keys e)))

  (get-reverse-ref-attrs [entity]
    (d/q '[:find [?attr ...]
           :in $ ?e
           :where
           [?a :db/valueType :db.type/ref]
           [?a :db/ident ?attr]
           [?r ?a ?e]]
         (d/entity-db entity)
         (:db/id entity)))

  dp/IKey
  (to-key [e]
    (datalog/make-entity-key e))

  dp/IRenderInline
  (render-inline [entity opt]
    (datalog/render-inline-entity entity opt))

  dp/IRenderDictionary
  (render-dictionary [entity opt]
    (hiccup/render-entries-dictionary entity opt (datalog/get-entity-entries entity))))
