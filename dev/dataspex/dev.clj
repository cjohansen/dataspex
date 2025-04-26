(ns dataspex.dev
  (:require [dataspex.core :as dataspex]
            [datomic.api :as d]))

(comment

  (set! *print-namespace-maps* false)

  (dataspex/inspect "Map"
    {:hello "World!"
     :runtime "JVM, baby!"
     :numbers (range 1500)})

  (dataspex/inspect "Map"
    {:greeting {:hello "World"}
     :runtime "JVM, baby!"
     :numbers (range 1500)})

  (def conn
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
                           :person/name "Wendy"}])

        conn)))

  (d/transact conn [{:person/id "pat"
                     :person/name "Pat"}])

  (d/transact conn [{:person/id "lisa"
                     :person/name "Lisa"}])

  (d/transact conn [{:person/id "bart"
                     :person/name "Bart"}])

  (dataspex/inspect "Datomic conn" conn)
  (dataspex/uninspect "Datomic conn")

  )
