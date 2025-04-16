(ns dataspex.datascript-test
  (:require [clojure.test :refer [deftest is testing]]
            [datascript.core :as d]
            [dataspex.data :as data]
            [dataspex.datascript :as datascript]
            [dataspex.helper :as h :refer [with-conn]]
            [dataspex.ui :as-alias ui]))

::datascript/keep

(def schema
  {:person/id {:db/cardinality :db.cardinality/one
               :db/unique :db.unique/identity}
   :person/friends {:db/valueType :db.type/ref
                    :db/cardinality :db.cardinality/many}})

(def data
  [{:person/id "bob"
    :person/name "Bob"
    :person/friends ["alice" "wendy"]}
   {:db/id "alice"
    :person/id "alice"
    :person/name "Alice"}
   {:db/id "wendy"
    :person/id "wendy"
    :person/name "Wendy"}])

(deftest navigation-test
  (testing "Navigates in connection"
    (is (with-conn [conn schema]
          (= (data/nav-in conn []) conn)))

    (is (= (with-conn [conn schema]
             (data/nav-in conn [:schema]))
           schema))

    (is (= (with-conn [conn schema]
             (data/nav-in conn [:schema :person/id]))
           {:db/cardinality :db.cardinality/one
            :db/unique :db.unique/identity}))

    (is (= (->> (with-conn [conn schema]
                  (d/transact! conn data)
                  (data/nav-in conn [:eavt]))
                count)
           8))

    (is (= (->> (with-conn [conn schema]
                  (d/transact! conn data)
                  (data/nav-in conn [:aevt]))
                count)
           8))

    (is (= (with-conn [conn schema]
             (d/transact! conn data)
             (data/nav-in conn [:max-eid]))
           3))

    (is (= (with-conn [conn schema]
             (d/transact! conn data)
             (data/nav-in conn [:max-tx]))
           536870913))

    (is (= (->> (with-conn [conn schema]
                  (data/nav-in conn [:rschema]))
                :db/unique)
           #{:db/ident :person/id})))

  (testing "Navigates in database"
    (is (= (with-conn [conn schema]
             (data/nav-in (d/db conn) [:schema]))
           schema)))

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

(deftest render-inline-test
  (testing "Renders connection inline"
    (is (= (with-conn [conn schema]
             (d/transact! conn data)
             (h/render-inline conn))
           [::ui/code "#datascript/Conn [3 entities, 8 datoms]"])))

  (testing "Renders database inline"
    (is (= (with-conn [conn schema]
             (d/transact! conn data)
             (h/render-inline (d/db conn)))
           [::ui/code "#datascript/DB [3 entities, 8 datoms]"])))

  (testing "Renders datom inline"
    (is (= (with-conn [conn schema]
             (d/transact! conn [{:person/id "wendy"}])
             (->> (first (:eavt @conn))
                  (h/render-inline
                   {:dataspex/inspectee "DS"
                    :dataspex/path [:eavt]})))
           [::ui/inline-tuple {::ui/prefix "datom"}
            [::ui/number
             {::ui/actions [[:dataspex.actions/assoc-in ["DS" :dataspex/path] [:eavt 1]]]}
             1]
            [::ui/keyword
             {::ui/actions [[:dataspex.actions/assoc-in ["DS" :dataspex/path] [:eavt :person/id]]]}
             :person/id]
            [::ui/string
             {::ui/actions [[:dataspex.actions/assoc-in ["DS" :dataspex/path] [:eavt 1 :person/id]]]}
             "wendy"]
            [::ui/number
             {::ui/actions [[:dataspex.actions/assoc-in ["DS" :dataspex/path] [:eavt 536870913]]]}
             536870913]
            [::ui/boolean true]])))

  (testing "Renders index inline"
    (is (= (->> (with-conn [conn schema]
                  (d/transact! conn [{:person/id "wendy"}])
                  (h/render-inline (:eavt @conn)))
                first)
           ::ui/set)))

  (testing "Renders large index inline"
    (is (= (with-conn [conn schema]
             (d/transact! conn data)
             (h/render-inline (:eavt @conn)))
           [::ui/link "#{8 Datoms}"])))

  (testing "Renders entity inline"
    (is (= (with-conn [conn schema]
             (d/transact! conn data)
             (h/render-inline (d/entity (d/db conn) 1)))
           [::ui/map
            [::ui/map-entry
             [::ui/keyword :person/id]
             [::ui/string "bob"]]]))))
