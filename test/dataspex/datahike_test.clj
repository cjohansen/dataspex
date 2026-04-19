(ns dataspex.datahike-test
  (:require [babashka.fs :as fs]
            [clojure.datafy :as datafy]
            [clojure.test :refer [deftest is testing]]
            [datahike.api :as d]
            [dataspex.data :as data]
            [dataspex.datahike :as datahike]
            [dataspex.datalog :as datalog]
            [dataspex.helper :as h :refer [with-conn]]
            [dataspex.ui :as-alias ui]
            [lookup.core :as lookup]))

::datahike/keep

(def schema
  [{:db/ident :movie/id
    :db/valueType :db.type/string
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one}
   {:db/ident :movie/title
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :person/id
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}
   {:db/ident :person/alias
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :person/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :person/boss
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident :person/friends
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}])

(def data
  [{:person/id "bob"
    :person/name "Bob"
    :person/friends ["alice" "wendy"]}
   {:db/id "alice"
    :person/id "alice"
    :person/boss "wendy"
    :person/name "Alice"}
   {:db/id "wendy"
    :person/id "wendy"
    :person/name "Wendy"}])

(defn create-config []
  (let [dir (fs/create-temp-dir)]
    {:store
     {:backend :file
      :id (random-uuid)
      :path (str dir "/db")}}))

(defn create-conn
  ([schema]
   (create-conn (create-config) schema))
  ([config schema]
   (d/create-database config)
   (let [conn (d/connect config)]
     (d/transact conn schema)
     conn)))

(defmacro with-conn
  {:clj-kondo/lint-as 'clojure.core/let}
  [[binding schema] & body]
  `(let [config# (create-config)
         ~binding (create-conn config# ~schema)
         res# (do ~@body)]
     (when (coll? res#)
       (doall res#))
     (d/delete-database config#)
     res#))

(deftest database-protocol-test
  (testing "count-entities-by-attr"
    (is (= (with-conn [conn schema]
             (d/transact conn data)
             (datalog/count-entities-by-attr (d/db conn) :person/id))
           3)))

  (testing "entity"
    (is (= (->> (with-conn [conn schema]
                  (d/transact conn data)
                  (datalog/entity (d/db conn) [:person/id "wendy"]))
                :person/name)
           "Wendy")))

  (testing "get-entities"
    (is (= (->> (with-conn [conn schema]
                  (d/transact conn data)
                  (datalog/get-entities (d/db conn)))
                (map :person/id))
           ["bob" "alice" "wendy"])))

  (testing "get-entities-by-attr"
    (is (= (->> (with-conn [conn schema]
                  (d/transact conn data)
                  (datalog/get-entities-by-attr (d/db conn) :person/id))
                (map :person/id))
           ["bob" "alice" "wendy"]))

    (is (= (->> (with-conn [conn schema]
                  (d/transact conn data)
                  (datalog/get-entities-by-attr (d/db conn) :person/friends))
                (map :person/id))
           ["bob"])))

  (testing "get-unique-attrs"
    (is (= (with-conn [conn schema]
             (datalog/get-unique-attrs (d/db conn)))
           [:movie/id :person/id])))

  (testing "get-attr-sort-val"
    (is (= (with-conn [conn schema]
             (datalog/get-attr-sort-val (d/db conn) :person/id))
           ["person" 0 0 0 "id"])))

  (testing "get-attrs-used-with"
    (is (= (with-conn [conn schema]
             (d/transact conn data)
             (datalog/get-attrs-used-with (d/db conn) :person/boss))
           [:person/id :person/boss :person/name]))))

(deftest entity-protocol-test
  (testing "entity-db"
    (with-conn [conn schema]
      (d/transact conn data)
      (let [db (d/db conn)]
        (is (= db (datalog/entity-db (d/entity db [:person/id "wendy"])))))))

  (testing "get-unique-entity-attrs"
    (is (= (with-conn [conn schema]
             (d/transact conn data)
             (->> (d/entity (d/db conn) [:person/id "wendy"])
                  datalog/get-unique-entity-attrs))
           [:person/id])))

  (testing "get-primitive-attrs"
    (is (= (with-conn [conn schema]
             (d/transact conn data)
             (->> (d/entity (d/db conn) [:person/id "wendy"])
                  datalog/get-primitive-attrs))
           [:person/id :person/name])))

  (testing "get-reverse-ref-attrs"
    (is (= (with-conn [conn schema]
             (d/transact conn data)
             (->> (d/entity (d/db conn) [:person/id "wendy"])
                  datalog/get-reverse-ref-attrs))
           [:person/friends :person/boss]))))

(with-conn [conn schema]
  (datahike/->SchemaKey (d/db conn)))

(deftest navigation-test
  (testing "Navigates in connection"
    (is (with-conn [conn schema]
          (= (data/nav-in conn []) conn)))

    (is (= (->> (with-conn [conn schema]
                  (data/nav-in conn [(datahike/->SchemaKey (d/db conn))]))
                datafy/datafy)
           schema))

    (is (= (with-conn [conn schema]
             (data/nav-in conn [(datahike/->SchemaKey (d/db conn)) :person/id]))
           {:db/ident :person/id
            :db/valueType :db.type/string
            :db/cardinality :db.cardinality/one
            :db/unique :db.unique/identity})))

  (testing "Navigates in database"
    (is (= (->> (with-conn [conn schema]
                  (data/nav-in (d/db conn) [(datahike/->SchemaKey (d/db conn))]))
                datafy/datafy)
           schema)))

  (testing "Navigates complicated path"
    (is (= (with-conn [conn schema]
             (d/transact conn data)
             (->> [(datalog/->EntitiesByAttrKey :person/id)
                   (datalog/->EntityKey 9 {})
                   :person/boss
                   :person/_boss]
                  (data/nav-in (d/db conn))
                  first
                  :db/id))
           9)))

  (testing "Makes entities keyable"
    (is (= (->> (with-conn [conn schema]
                  (d/transact conn data)
                  (data/get-set-entries
                   #{(d/entity (d/db conn) [:person/id "bob"])}
                   {}))
                first
                :k
                data/inspect)
           {:person/id "bob"})))

  (testing "Navigates in entity"
    (is (= (with-conn [conn schema]
             (d/transact conn data)
             (let [k (data/as-key (d/entity (d/db conn) [:person/id "wendy"]))]
               (-> (d/entity (d/db conn) [:person/id "bob"])
                   (data/nav-in [:person/friends k :person/name]))))
           "Wendy")))

  (testing "Navigates with entity as map key"
    (is (= (with-conn [conn schema]
             (d/transact conn data)
             (let [entity (d/entity (d/db conn) [:person/id "wendy"])
                   k (data/as-key entity)]
               (data/nav-in {entity "Hello"} [k])))
           "Hello")))

  (testing "Navigates reverse ref from entity"
    (is (= (with-conn [conn schema]
             (d/transact conn data)
             (let [friend (d/entity (d/db conn) [:person/id "bob"])]
               (-> (d/entity (d/db conn) [:person/id "wendy"])
                   (data/nav-in [:person/_friends (data/as-key friend) :person/name]))))
           "Bob"))))

(deftest render-inline-test
  (testing "Renders connection inline"
    (is (= (with-conn [conn schema]
             (d/transact conn data)
             (h/render-inline conn))
           [::ui/code "#datahike.connector.Connection [12 entities, 34 datoms]"])))

  (testing "Renders database inline"
    (is (= (with-conn [conn schema]
             (d/transact conn data)
             (h/render-inline (d/db conn)))
           [::ui/code "#datahike.db.Db [12 entities, 34 datoms]"])))

  (testing "Renders entity inline"
    (is (= (with-conn [conn schema]
             (d/transact conn data)
             (h/render-inline (d/entity (d/db conn) [:person/id "bob"])))
           [::ui/map {::ui/prefix "entity"}
            [::ui/map-entry
             [::ui/keyword :person/id]
             [::ui/string "bob"]]
            [::ui/map-entry
             [::ui/keyword :person/name]
             [::ui/string "Bob"]]])))

  (testing "Renders entity with no primitive attributes inline"
    (is (= (with-conn [conn schema]
             (->> [{:db/id "alice"
                    :person/id "alice"
                    :person/boss
                    {:db/id "anonymous"
                     :person/friends
                     [{:person/id "bob"
                       :person/name "Bob"}
                      {:person/id "wendy"
                       :person/name "Wendy"}]}}]
                  (d/transact conn))
             (->> [:person/id "alice"]
                  (d/entity (d/db conn))
                  :person/boss
                  h/render-inline))
           [::ui/map {::ui/prefix "entity"}
            [::ui/map-entry [::ui/keyword :person/friends]]]))))

(deftest render-dictionary-test
  (testing "Renders connection as dictionary"
    (is (= (->> (with-conn [conn schema]
                  (h/render-dictionary conn))
                lookup/children
                (mapv (comp first lookup/children))
                (mapv lookup/text))
           ["Schema"
            "Entities"
            "Transactions"])))

  (testing "Does not count deleted entities"
    (is (= (->> (with-conn [conn schema]
                  (d/transact conn data)
                  (d/transact conn [[:db/retractEntity [:person/id "bob"]]])
                  (h/render-dictionary conn))
                (lookup/select-one [:dataspex.ui/ul])
                lookup/children
                (mapv lookup/text))
           ["All (2)" ":person/id  (2)"])))

  (testing "Offers entity filters by unique attribute"
    (is (= (->> (with-conn [conn schema]
                  (->> data
                       (concat [{:movie/id "batman"
                                 :movie/title "Batman Forever"}])
                       (d/transact conn))
                  (h/render-dictionary {:dataspex/inspectee "DB"} conn))
                lookup/children
                second
                (lookup/select :dataspex.ui/ul)
                lookup/text)
           "All (4) :movie/id  (1) :person/id  (3)")))

  (testing "Renders database as dictionary"
    (is (= (->> (with-conn [conn schema]
                  (h/render-dictionary (d/db conn)))
                lookup/children
                (mapv (comp first lookup/children))
                (mapv lookup/text))
           ["Schema"
            "Entities"
            "Transactions"])))

  (testing "Renders entity as dictionary"
    (is (= (->> (with-conn [conn schema]
                  (d/transact conn data)
                  (h/render-dictionary (d/entity (d/db conn) [:person/id "bob"])))
                (lookup/select '[::ui/dictionary > ::ui/entry])
                (mapv (comp first lookup/children))
                (mapv lookup/text))
           [":db/id"
            ":person/id"
            ":person/name"
            ":person/friends"])))

  (testing "Renders reverse refs as underscore attributes"
    (is (= (->> (with-conn [conn schema]
                  (d/transact conn data)
                  (->> (d/entity (d/db conn) [:person/id "wendy"])
                       h/render-dictionary))
                (lookup/select '[::ui/dictionary > ::ui/entry])
                (mapv (comp first lookup/children))
                (mapv lookup/text))
           [":db/id"
            ":person/id"
            ":person/name"
            ":person/_boss"
            ":person/_friends"])))

  (testing "Renders many ref as navigatable dictionary"
    (is (= (with-conn [conn schema]
             (d/transact conn data)
             (let [data (:person/friends (d/entity (d/db conn) [:person/id "bob"]))]
               (->> data
                    (h/render-dictionary
                     {:dataspex/inspectee "DB"})
                    (lookup/select '[::ui/entry])
                    first
                    lookup/attrs
                    ::ui/actions
                    first
                    last
                    (data/nav-in data)
                    (into {}))))
           {:person/id "wendy"
            :person/name "Wendy"})))

  (testing "Renders reverse refs to entity below regular keys"
    (is (= (->> (with-conn [conn schema]
                  (d/transact conn data)
                  (h/render-dictionary (d/entity (d/db conn) [:person/id "alice"])))
                (lookup/select '[::ui/dictionary > ::ui/entry])
                (mapv (comp first lookup/children))
                (mapv lookup/text))
           [":db/id"
            ":person/id"
            ":person/name"
            ":person/boss"
            ":person/_friends"]))))

(deftest key-table-keys
  (testing "Prefers keys with same namespace as uniqueness attribute"
    (is (= (with-conn [conn schema]
             (d/transact conn [{:db/ident :employee/number
                                :db/valueType :db.type/string
                                :db/cardinality :db.cardinality/one}])
             (d/transact conn [{:person/id "alice"
                                :person/name "Alice"
                                :employee/number "101"}])
             (datalog/get-table-keys (d/db conn) :person/id))
           [:person/id :person/name :employee/number]))))
