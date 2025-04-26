(ns dataspex.datomic
  (:require [clojure.core.protocols :as p]
            [dataspex.data :as data]
            [dataspex.datalog :as datalog]
            [dataspex.hiccup :as hiccup]
            [dataspex.inspector :as inspector]
            [dataspex.protocols :as dp]
            [dataspex.ui :as-alias ui]
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
    (hiccup/render-entries-dictionary
     schema
     (mapv (fn [attr] {:k attr :v attr}) schema)
     opt)))

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

(defn render-database-dictionary [db opt]
  (hiccup/render-entries-dictionary
   db
   [{:k (->SchemaKey db)
     :label 'Schema
     :v (->Schema db)}
    {:label 'Entities
     :v (datalog/->EntityIndex db)}]
   opt))

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
         distinct))

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
    (hiccup/render-entries-dictionary entity (datalog/get-entity-entries entity) opt)))

(comment

  (let [uri "datomic:mem://dataspex"]
    (d/create-database uri)
    (let [conn (d/connect uri)]
      (d/transact conn [{:db/ident :person/id
                         :db/valueType :db.type/string
                         :db/unique :db.unique/identity
                         :db/cardinality :db.cardinality/one}
                        {:db/ident :person/name
                         :db/valueType :db.type/string
                         :db/cardinality :db.cardinality/one}
                        {:db/ident :person/friends
                         :db/valueType :db.type/ref
                         :db/cardinality :db.cardinality/many}])

      (d/transact conn [{:person/id "bob"
                         :person/name "Bob"
                         :person/friends ["alice" "wendy"]}
                        {:db/id "alice"
                         :person/id "alice"
                         :person/name "Alice"}
                        {:db/id "wendy"
                         :person/id "wendy"
                         :person/name "Wendy"}])))

  (def conn (d/connect "datomic:mem://dataspex"))
  (def db (d/db conn))
  (def bob (d/entity db [:person/id "bob"]))

  (inspector/inspect dataspex.server/store "Bob" bob)

  (def alice (d/entity db [:person/id "alice"]))

  (datalog/get-ref-attrs bob)
  (datalog/get-primitive-attrs bob)

  (type (d/entity db [:person/id "bob"]))
  datomic.query.EntityMap

  (datalog/get-attr-sort-val db :person/id)
  (datalog/get-attr-sort-val db :person/friends)
  (datalog/get-attr-sort-val db :person/name)

  (into {} (d/entity db :person/id))

  (datalog/get-reverse-ref-attrs alice)

  (type db)

  datomic.db.Db

  (set! *print-namespace-maps* false)

)
