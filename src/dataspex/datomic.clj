(ns dataspex.datomic
  (:require [dataspex.datalog :as datalog]
            [dataspex.hiccup :as hiccup]
            [dataspex.inspector :as inspector]
            [dataspex.protocols :as dp]
            [datomic.api :as d]))

(extend-type datomic.db.Db
  datalog/Database
  (entity [db entity-ref]
    (d/entity db entity-ref))

  (get-attr-sort-val [db a]
    (let [attribute (d/entity db a)]
      [(if (:db/unique attribute)
         0 1)
       (if (= :db.type/ref (:db/valueType attribute))
         1 0)
       (if (= :db.cardinality/many (:db/cardinality attribute))
         1 0)
       a]))

  dp/INavigatable
  (nav-in [db path]
    (datalog/nav-in-db db path)))

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
           (not [?attr :db/unique])
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
