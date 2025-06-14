(ns dataspex.datascript-test
  (:require [clojure.test :refer [deftest is testing]]
            [datascript.core :as d]
            [dataspex.actions :as-alias actions]
            [dataspex.data :as data]
            [dataspex.datalog :as datalog]
            [dataspex.datascript :as datascript]
            [dataspex.diff :as diff]
            [dataspex.helper :as h :refer [with-conn]]
            [dataspex.ui :as-alias ui]
            [lookup.core :as lookup]))

::datascript/keep

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

(deftest get-primitive-attrs-test
  (testing "Gets primitive attributes from very simple database"
    (is (= (let [conn (d/create-conn {:form/id {:db/unique :db.unique/identity}})]
             (d/transact! conn [{:form/id "form"}])
             (datalog/get-primitive-attrs (d/entity (d/db conn) [:form/id "form"])))
           [:form/id]))))

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
           9))

    (is (= (->> (with-conn [conn schema]
                  (d/transact! conn data)
                  (data/nav-in conn [:aevt]))
                count)
           9))

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
           #{:db/ident :person/id :movie/id})))

  (testing "Navigates in database"
    (is (= (with-conn [conn schema]
             (data/nav-in (d/db conn) [:schema]))
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
           [::ui/code "#datascript/Conn [3 entities, 9 datoms]"])))

  (testing "Renders database inline"
    (is (= (with-conn [conn schema]
             (d/transact! conn data)
             (h/render-inline (d/db conn)))
           [::ui/code "#datascript/DB [3 entities, 9 datoms]"])))

  (testing "Renders datom inline"
    (is (= (with-conn [conn schema]
             (d/transact! conn [{:person/id "wendy"}])
             (->> (first (:eavt @conn))
                  (h/render-inline
                   {:dataspex/inspectee "DS"
                    :dataspex/path [:eavt]
                    :dataspex/activity :dataspex.activity/browse})))
           [::ui/inline-tuple {::ui/prefix "datom"}
            [::ui/number
             {::ui/actions [[::actions/navigate "DS"
                             [:eavt (datalog/->EntityId 1)]]]}
             1]
            [::ui/keyword
             {::ui/actions [[::actions/navigate "DS"
                             [:eavt (datalog/->Attr :person/id)]]]}
             :person/id]
            [::ui/string
             {::ui/actions [[::actions/navigate "DS"
                             [:eavt
                              (datalog/->EntityId 1)
                              (datalog/->Attr :person/id)
                              (datalog/->AttrValue :person/id "wendy")]]]}
             "wendy"]
            [::ui/number
             {::ui/actions [[::actions/navigate "DS"
                             [:eavt (datalog/->EntityId 536870913)]]]}
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
           [::ui/link "#{9 Datoms}"])))

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
            "Entities"
            "Datoms by entity (eavt)"
            ":aevt"
            ":avet"
            ":max-eid"
            ":max-tx"
            ":rschema"
            ":hash"])))

  (testing "Offers entity filters by unique attribute"
    (is (= (->> (with-conn [conn schema]
                  (->> data
                       (concat [{:movie/id "batman"
                                 :movie/title "Batman Forever"}])
                       (d/transact! conn))
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
           9)))

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
            ":person/name"
            ":person/boss"
            ":person/_friends"]))))

(deftest render-source-test
  (testing "Renders connection as source"
    (is (= (->> (with-conn [conn schema]
                  (d/transact! conn data)
                  (h/render-source conn))
                (lookup/select ::ui/tuple)
                count)
           9)))

  (testing "Renders database as source"
    (is (= (->> (with-conn [conn schema]
                  (d/transact! conn data)
                  (h/render-source (d/db conn)))
                (lookup/select ::ui/tuple)
                count)
           9))))

(deftest diff-test
  (testing "Diffs two datascript databases"
    (is (= (-> (with-conn [conn schema]
                 (d/transact! conn data)
                 (let [a (d/db conn)]
                   (d/transact! conn [{:person/id "bob"
                                       :person/alias "Notorious B-O-B"}])
                   (diff/diff a (d/db conn))))
               h/undatom-diff)
           [[[0] :+ [1 :person/alias "Notorious B-O-B" 536870914 true]]]))))


;; "+2/-1 entities, +2/-3 attributes in 4 entities"
