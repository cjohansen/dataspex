(ns dataspex.datalevin-test
  (:require [babashka.fs :as fs]
            [clojure.datafy :as datafy]
            [clojure.test :refer [deftest is testing]]
            [datalevin.core :as d]
            [dataspex.data :as data]
            [dataspex.datalevin :as datalevin]
            [dataspex.datalog :as datalog]
            [dataspex.helper :as h]
            [dataspex.ui :as ui]
            [lookup.core :as lookup]))

(def schema
  {:person/id {:db/cardinality :db.cardinality/one
               :db/unique :db.unique/identity}
   :person/friends {:db/valueType :db.type/ref
                    :db/cardinality :db.cardinality/many}
   :person/boss {:db/valueType :db.type/ref}
   :movie/id {:db/unique :db.unique/identity}})

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

(defn create-conn [schema]
  (let [dir (fs/create-temp-dir)]
    (d/create-conn (str dir) schema)))

(deftest get-primitive-attrs-test
  (testing "Gets primitive attributes from very simple database"
    (is (= (let [conn (create-conn {:form/id {:db/unique :db.unique/identity}})]
             (d/transact! conn [{:form/id "form"}])
             (datalog/get-primitive-attrs (d/entity (d/db conn) [:form/id "form"])))
           [:form/id]))))

(defmacro with-conn
  {:clj-kondo/lint-as 'clojure.core/let}
  [[binding schema] & body]
  `(let [~binding (create-conn ~schema)
         res# (with-redefs [dataspex.data/inspectors (atom [dataspex.datalevin/reify-conn])]
                ~@body)]
     (when (coll? res#)
       (doall res#))
     (d/clear ~binding)
     res#))

(deftest navigation-test
  (testing "Navigates in connection"
    (is (with-conn [conn schema]
          (= (data/nav-in conn []) conn)))

    (is (= (-> (with-conn [conn schema]
                 (data/nav-in conn [(datalevin/->SchemaKey (d/db conn))]))
               datafy/datafy
               (dissoc :db/created-at :db/updated-at :db/ident)
               (update-vals #(dissoc % :db/aid)))
           schema))

    (is (= (-> (with-conn [conn schema]
                 (data/nav-in conn [(datalevin/->SchemaKey (d/db conn)) :person/id]))
               (dissoc :db/aid))
           {:db/cardinality :db.cardinality/one
            :db/unique :db.unique/identity})))

  (testing "Navigates in database"
    (is (= (-> (with-conn [conn schema]
                 (let [db (d/db conn)]
                   (data/nav-in db [(datalevin/->Schema db)])))
               (dissoc :db/created-at :db/updated-at :db/ident)
               (update-vals #(dissoc % :db/aid)))
           schema)))

  (testing "Navigates complicated path"
    (is (= (with-conn [conn schema]
             (d/transact! conn data)
             (->> [(datalog/->EntitiesByAttrKey :person/id)
                   (datalog/->EntityKey 2 {})
                   :person/boss
                   :person/_boss]
                  (data/nav-in (d/db conn))
                  first
                  :db/id))
           2)))

  (testing "Makes entities keyable"
    (is (= (->> (with-conn [conn schema]
                  (d/transact! conn data)
                  (data/get-set-entries #{(d/entity (d/db conn) 1)} {}))
                first
                :k
                data/inspect)
           {:person/id "bob"})))

  (testing "Navigates in entity"
    (is (= (with-conn [conn schema]
             (d/transact! conn data)
             (let [k (data/as-key (d/entity (d/db conn) 3))]
               (data/nav-in (d/entity (d/db conn) 1) [:person/friends k :person/name])))
           "Wendy")))

  (testing "Navigates with entity as map key"
    (is (= (with-conn [conn schema]
             (d/transact! conn data)
             (let [entity (d/entity (d/db conn) 3)
                   k (data/as-key entity)]
               (data/nav-in {entity "Hello"} [k])))
           "Hello")))

  (testing "Navigates reverse ref from entity"
    (is (= (with-conn [conn schema]
             (d/transact! conn data)
             (let [friend (d/entity (d/db conn) 1)]
               (data/nav-in (d/entity (d/db conn) 3) [:person/_friends (data/as-key friend) :person/name])))
           "Bob"))))

(deftest lookupable-test
  (testing "Entities can be looked up"
    (is (with-conn [conn schema]
          (d/transact! conn data)
          (->> (d/entity (d/db conn) [:person/id "wendy"])
               data/lookupable?)))))

(deftest render-inline-test
  (testing "Renders connection inline"
    (is (= (with-conn [conn schema]
             (d/transact! conn data)
             (h/render-inline conn))
           [::ui/code "#datalevin/Conn [3 entities]"])))

  (testing "Renders database inline"
    (is (= (with-conn [conn schema]
             (d/transact! conn data)
             (h/render-inline (d/db conn)))
           [::ui/code "#datalevin/DB [3 entities]"])))

  (testing "Renders entity inline"
    (is (= (with-conn [conn schema]
             (d/transact! conn data)
             (h/render-inline (d/entity (d/db conn) 1)))
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
                    {:person/friends
                     [{:person/id "bob"
                       :person/name "Bob"}]}}]
                  (d/transact! conn))
             (->> [:person/id "alice"]
                  (d/entity (d/db conn))
                  :person/boss
                  h/render-inline))
           [::ui/map {::ui/prefix "entity"}
            [::ui/map-entry
             [::ui/keyword :person/friends]
             [::ui/set
              [::ui/map {::ui/prefix "entity"}
               [::ui/map-entry
                [::ui/keyword :person/id]
                [::ui/string "bob"]]]]]]))))

(deftest render-dictionary-test
  (testing "Renders connection as dictionary"
    (is (= (->> (with-conn [conn schema]
                  (h/render-dictionary conn))
                lookup/children
                (mapv (comp first lookup/children))
                (mapv lookup/text))
           ["Schema"
            "Entities"])))

  (testing "Offers entity filters by unique attribute"
    (is (= (->> (with-conn [conn schema]
                  (->> data
                       (concat [{:movie/id "batman"
                                 :movie/title "Batman Forever"}])
                       (d/transact! conn))
                  (h/render-dictionary {:dataspex/inspectee "DB"} (d/db conn)))
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
            "Entities"])))

  (testing "Renders entity as dictionary"
    (is (= (->> (with-conn [conn schema]
                  (d/transact! conn data)
                  (h/render-dictionary (d/entity (d/db conn) 1)))
                (lookup/select '[::ui/dictionary > ::ui/entry])
                (mapv (comp first lookup/children))
                (mapv lookup/text))
           [":db/id"
            ":person/id"
            ":person/name"
            ":person/friends"])))

  (testing "Renders reverse refs as underscore attributes"
    (is (= (->> (with-conn [conn schema]
                  (d/transact! conn data)
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
             (d/transact! conn data)
             (let [data (:person/friends (d/entity (d/db conn) 1))]
               (->> data
                    (h/render-dictionary
                     {:dataspex/inspectee "DB"})
                    lookup/children
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
                  (d/transact! conn data)
                  (h/render-dictionary (d/entity (d/db conn) 2)))
                (lookup/select '[::ui/dictionary > ::ui/entry])
                (mapv (comp first lookup/children))
                (mapv lookup/text))
           [":db/id"
            ":person/id"
            ":person/name"
            ":person/boss"
            ":person/_friends"])))

  (testing "Doesn't treat any occurrence of entity id in value position as a reverse ref"
    (is (= (->> (with-conn [conn schema]
                  (d/transact! conn (conj data
                                          {:person/id 5
                                           :person/luls 2}))
                  (h/render-dictionary (d/entity (d/db conn) 2)))
                (lookup/select '[::ui/dictionary > ::ui/entry])
                (mapv (comp first lookup/children))
                (mapv lookup/text))
           [":db/id"
            ":person/id"
            ":person/luls"
            ":person/name"
            ":person/boss"
            ":person/_friends"]))))
