(ns dataspex.test.data
  (:require [datascript.core :as d]))

(def schema
  {:person/id {:db/cardinality :db.cardinality/one
               :db/unique :db.unique/identity}
   :person/friends {:db/valueType :db.type/ref
                    :db/cardinality :db.cardinality/many}
   :employee/boss {:db/valueType :db.type/ref}})

(def people
  [{:db/id "bob"
    :person/id "bob"
    :person/name "Bob"
    :person/friends ["alice" "wendy"]}
   {:db/id "alice"
    :person/id "alice"
    :person/name "Alice"
    :employee/boss "wendy"}
   {:db/id "wendy"
    :person/id "wendy"
    :person/name "Wendy"}])

(defonce conn
  (let [conn (d/create-conn schema)]
    (d/transact! conn people)
    conn))

(defonce tiny-conn
  (let [conn (d/create-conn schema)]
    (d/transact!
     conn
     [{:person/id "wendy"
       :person/name "Wendy"}])
    conn))
