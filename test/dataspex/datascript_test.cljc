(ns dataspex.datascript-test
  (:require [clojure.test :refer [deftest is testing]]
            [datascript.core :as d]
            [dataspex.actions :as-alias actions]
            [dataspex.data :as data]
            [dataspex.datascript :as datascript]
            [dataspex.helper :as h :refer [with-conn]]
            [dataspex.ui :as-alias ui]
            [lookup.core :as lookup]))

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

(deftest render-dictionary-test
  (testing "Renders connection as dictionary"
    (is (= (->> (with-conn [conn schema]
                  (h/render-dictionary conn))
                lookup/children
                (mapv (comp first lookup/children))
                (mapv lookup/text))
           ["Schema"
            "Entities"
            "Datoms by entity (eavt)"
            ":aevt"
            ":avet"
            ":max-eid"
            ":max-tx"
            ":rschema"
            ":hash"])))

  (testing "Renders database as dictionary"
    (is (= (->> (with-conn [conn schema]
                  (h/render-dictionary (d/db conn)))
                lookup/children
                (mapv (comp first lookup/children))
                (mapv lookup/text))
           ["Schema"
            "Entities"
            "Datoms by entity (eavt)"
            ":aevt"
            ":avet"
            ":max-eid"
            ":max-tx"
            ":rschema"
            ":hash"])))

  (testing "Renders datom as dictionary"
    (is (= (->> (with-conn [conn schema]
                  (d/transact! conn [{:person/id "wendy"}])
                  (->> (first (:eavt @conn))
                       (h/render-dictionary
                        {:dataspex/inspectee "DB"
                         :dataspex/path [:eavt]})))
                (lookup/select ::ui/entry)
                (mapv (fn [entry]
                        [(lookup/text (second (lookup/children entry)))
                         (-> entry lookup/attrs ::ui/actions first last)])))
           [["1" [:eavt 1]]
            [":person/id" [:eavt :person/id]]
            ["wendy" [:eavt 1 :person/id]]
            ["536870913" [:eavt 536870913]]
            ["true" nil]])))

  (testing "Renders index as dictionary"
    (is (= (->> (with-conn [conn schema]
                  (d/transact! conn data)
                  (->> (:eavt @conn)
                       (h/render-dictionary {:dataspex/path [:eavt]})))
                (lookup/select ::ui/tuple)
                count)
           8)))

  (testing "Renders entity as dictionary"
    (is (= (->> (with-conn [conn schema]
                  (d/transact! conn data)
                  (h/render-dictionary (d/entity (d/db conn) 1)))
                (lookup/select '[::ui/dictionary > ::ui/entry])
                (mapv (comp first lookup/children))
                (mapv lookup/text))
           [":person/id"
            ":person/name"
            ":person/friends"])))

  (testing "Renders many ref as navigatable dictionary"
    (is (= (with-conn [conn schema]
             (d/transact! conn data)
             (let [data (:person/friends (d/entity (d/db conn) 1))]
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
           {:person/id "alice"
            :person/name "Alice"})))

  (testing "Renders reverse refs to entity below regular keys"
    (is (= (->> (with-conn [conn schema]
                  (d/transact! conn data)
                  (h/render-dictionary (d/entity (d/db conn) 2)))
                (lookup/select '[::ui/dictionary > ::ui/entry])
                (mapv (comp first lookup/children))
                (mapv lookup/text))
           [":person/id"
            ":person/name"
            ":person/_friends"]))))
