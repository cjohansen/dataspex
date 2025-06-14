(ns dataspex.datomic-test
  (:require [clojure.datafy :as datafy]
            [clojure.test :refer [deftest is testing]]
            [dataspex.data :as data]
            [dataspex.datalog :as datalog]
            [dataspex.datomic :as datomic]
            [dataspex.helper :as h]
            [dataspex.inspector :as inspector]
            [dataspex.protocols :as dp]
            [dataspex.ui :as-alias ui]
            [datomic.api :as d]
            [lookup.core :as lookup]))

::datomic/keep

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

(defmacro with-conn
  {:clj-kondo/lint-as 'clojure.core/let}
  [[binding] & body]
  `(let [uri# ~(str "datomic:mem://" (random-uuid))]
     (d/delete-database uri#)
     (d/create-database uri#)
     (let [~binding (d/connect uri#)]
       @(d/transact ~binding (into [{:db/id "datomic.tx"
                                     :db/txInstant #inst "2025-04-26T16:00:00"}]
                                   schema))
       ~@body)))

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

(deftest navigation-test
  (testing "Navigates in connection"
    (is (with-conn [conn schema]
          (= (data/nav-in conn []) conn)))

    (is (= (->> (with-conn [conn schema]
                  (data/nav-in conn [(datomic/->SchemaKey (d/db conn))]))
                datafy/datafy)
           schema))

    (is (= (with-conn [conn schema]
             (data/nav-in conn [(datomic/->SchemaKey (d/db conn)) :person/id]))
           {:db/ident :person/id
            :db/valueType :db.type/string
            :db/cardinality :db.cardinality/one
            :db/unique :db.unique/identity})))

  (testing "Navigates in database"
    (is (= (->> (with-conn [conn schema]
                  (data/nav-in (d/db conn) [(datomic/->SchemaKey (d/db conn))]))
                datafy/datafy)
           schema)))

  (testing "Navigates complicated path"
    (is (= (with-conn [conn schema]
             @(d/transact conn data)
             (->> [(datalog/->EntitiesByAttrKey :person/id)
                   (datalog/->EntityKey 17592186045419 {})
                   :person/boss
                   :person/_boss]
                  (data/nav-in (d/db conn))
                  first
                  :db/id))
           17592186045419)))

  (testing "Makes entities keyable"
    (is (= (->> (with-conn [conn schema]
                  @(d/transact conn data)
                  (data/get-set-entries
                   #{(d/entity (d/db conn) [:person/id "bob"])}
                   {}))
                first
                :k
                data/inspect)
           {:person/id "bob"})))

  (testing "Navigates in entity"
    (is (= (with-conn [conn schema]
             @(d/transact conn data)
             (let [k (data/as-key (d/entity (d/db conn) [:person/id "wendy"]))]
               (-> (d/entity (d/db conn) [:person/id "bob"])
                   (data/nav-in [:person/friends k :person/name]))))
           "Wendy")))

  (testing "Navigates with entity as map key"
    (is (= (with-conn [conn schema]
             @(d/transact conn data)
             (let [entity (d/entity (d/db conn) [:person/id "wendy"])
                   k (data/as-key entity)]
               (data/nav-in {entity "Hello"} [k])))
           "Hello")))

  (testing "Navigates reverse ref from entity"
    (is (= (with-conn [conn schema]
             @(d/transact conn data)
             (let [friend (d/entity (d/db conn) [:person/id "bob"])]
               (-> (d/entity (d/db conn) [:person/id "wendy"])
                   (data/nav-in [:person/_friends (data/as-key friend) :person/name]))))
           "Bob"))))

(let [[e a v t add?]
      (with-conn [conn schema]
        @(d/transact conn [{:person/id "wendy"}])
        (->> (d/datoms (d/db conn) :eavt)
             first
             ))]
  [e a v t add?])

(with-conn [conn schema]
  (:db/valueType (d/entity (d/db conn) :person/boss)))

(deftest render-inline-test
  (testing "Renders connection inline"
    (is (= (with-conn [conn schema]
             @(d/transact conn data)
             (h/render-inline conn))
           [::ui/code "#datomic.Connection [309 datoms]"])))

  (testing "Renders database inline"
    (is (= (with-conn [conn schema]
             @(d/transact conn data)
             (h/render-inline (d/db conn)))
           [::ui/code "#datomic.db.Db [309 datoms]"])))

  (testing "Renders entity inline"
    (is (= (with-conn [conn schema]
             @(d/transact conn data)
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
                  (d/transact conn)
                  deref)
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
                  @(d/transact conn data)
                  @(d/transact conn [[:db/retractEntity [:person/id "bob"]]])
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
                       (d/transact conn)
                       deref)
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
                  @(d/transact conn data)
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
                  @(d/transact conn data)
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
             @(d/transact conn data)
             (let [data (:person/friends (d/entity (d/db conn) [:person/id "bob"]))]
               (->> data
                    (h/render-dictionary
                     {:dataspex/inspectee "DB"})
                    (lookup/select '[::ui/entry])
                    second
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
                  @(d/transact conn data)
                  (h/render-dictionary (d/entity (d/db conn) [:person/id "alice"])))
                (lookup/select '[::ui/dictionary > ::ui/entry])
                (mapv (comp first lookup/children))
                (mapv lookup/text))
           [":db/id"
            ":person/id"
            ":person/name"
            ":person/boss"
            ":person/_friends"]))))

(deftest diff-test
  (testing "Converts tx-data to a dataspex diff"
    (is (= (->> (with-conn [conn schema]
                  @(d/transact conn (into [{:db/id "datomic.tx"
                                            :db/txInstant #inst "2025-04-26T16:01:00"}]
                                          data))
                  @(d/transact conn [{:db/id "datomic.tx"
                                      :db/txInstant #inst "2025-04-26T16:02:00"}
                                     {:person/id "bob"
                                      :person/alias "Notorious B-O-B"}]))
                :tx-data
                datomic/tx-data->diff
                h/undatom-diff)
           [[[13194139534317] :+ [13194139534317 50 #inst "2025-04-26T16:02:00.000-00:00" 13194139534317 true]]
            [[17592186045418] :+ [17592186045418 75 "Notorious B-O-B" 13194139534317 true]]]))))

(deftest inspect-test
  (testing "Inspect watches Datomic conn for updates"
    (is (= (with-conn [conn schema]
             (let [dataspex-store (atom {})]
               @(d/transact conn (into [{:db/id "datomic.tx"
                                         :db/txInstant #inst "2025-04-26T16:01:00"}]
                                       data))
               (inspector/inspect dataspex-store "Datomic conn" conn)
               @(d/transact conn [{:db/id "datomic.tx"
                                   :db/txInstant #inst "2025-04-26T16:02:00"}
                                  {:person/id "bob"
                                   :person/alias "Notorious B-O-B"}])
               (Thread/sleep 50)
               (let [{:keys [ref subscription history]} (get-in @dataspex-store ["Datomic conn"])]
                 (dp/unwatch ref subscription)
                 (for [v history]
                   (-> v
                       (dissoc :created-at)
                       (update :val (comp :datoms d/db-stats))
                       (update :diff h/undatom-diff))))))
           [{:rev 2
             :val 311
             :diff [[[13194139534317] :+ [13194139534317 50 #inst "2025-04-26T16:02:00" 13194139534317 true]]
                    [[17592186045418] :+ [17592186045418 75 "Notorious B-O-B" 13194139534317 true]]]}
            {:rev 1
             :val 309
             :diff []}]))))

(deftest key-table-keys
  (testing "Prefers keys with same namespace as uniqueness attribute"
    (is (= (with-conn [conn schema]
             @(d/transact conn [{:db/ident :employee/number
                                 :db/valueType :db.type/string
                                 :db/cardinality :db.cardinality/one}])
             @(d/transact conn [{:person/id "alice"
                                 :person/name "Alice"
                                 :employee/number "101"}])
             (datalog/get-table-keys (d/db conn) :person/id))
           [:person/id :person/name :employee/number]))))
