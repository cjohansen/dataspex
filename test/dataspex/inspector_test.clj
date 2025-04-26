(ns dataspex.inspector-test
  (:require [clojure.test :refer [deftest is testing]]
            [datascript.core :as d]
            [dataspex.helper :as h]
            [dataspex.inspector :as inspector]))

(def dataspex-opts
  {:now #inst "2025-04-16T16:19:58"
   :track-changes? true
   :history-limit 3})

(def apr16-1620 (assoc dataspex-opts :now #inst "2025-04-16T16:20:07"))
(def apr16-1621 (assoc dataspex-opts :now #inst "2025-04-16T16:21:14"))
(def apr16-1622 (assoc dataspex-opts :now #inst "2025-04-16T16:22:05"))
(def apr16-1625 (assoc dataspex-opts :now #inst "2025-04-16T16:25:32"))

(def data
  {:movie/title "Interstellar"
   :movie/year 2014})

(deftest inspect-val-test
  (testing "Initializes view options for inspected value"
    (is (= (inspector/inspect-val nil data dataspex-opts)
           {:dataspex/path []
            :dataspex/activity :dataspex.activity/browse
            :rev 1
            :val data
            :history [{:created-at #inst "2025-04-16T16:19:58"
                       :val data
                       :rev 1}]})))

  (testing "Does not change existing view options"
    (is (= (-> {:dataspex/path [:movies]
                :dataspex/activity :dataspex.activity/audit
                :dataspex/pagination {:page-size 10}
                :dataspex/folding {[:movies] {:folded? false}}
                :dataspex/sorting {[:movies] {:key :movie/title}}}
               (inspector/inspect-val data dataspex-opts)
               (dissoc :rev :val :history))
           {:dataspex/path [:movies]
            :dataspex/activity :dataspex.activity/audit
            :dataspex/pagination {:page-size 10}
            :dataspex/folding {[:movies] {:folded? false}}
            :dataspex/sorting {[:movies] {:key :movie/title}}})))

  (testing "Updates rev and value on second inspect"
    (is (= (-> (inspector/inspect-val nil data apr16-1620)
               (inspector/inspect-val (dissoc data :movie/year) apr16-1621)
               (select-keys [:rev :val]))
           {:rev 2
            :val {:movie/title "Interstellar"}})))

  (testing "Ignores multiple inspects of exact same value"
    (is (= (-> (inspector/inspect-val nil data apr16-1620)
               (inspector/inspect-val data apr16-1621)
               (select-keys [:rev :val :history]))
           {:rev 1
            :val data
            :history
            [{:created-at #inst "2025-04-16T16:20:07.000-00:00"
              :rev 1
              :val data}]})))

  (testing "Tracks diffs between versions"
    (is (= (-> (inspector/inspect-val nil data apr16-1620)
               (inspector/inspect-val (dissoc data :movie/year) apr16-1621)
               :history)
           [{:created-at #inst "2025-04-16T16:21:14.000-00:00"
             :rev 2
             :val {:movie/title "Interstellar"}
             :diff [[[:movie/year] :- 2014]]}
            {:created-at #inst "2025-04-16T16:20:07.000-00:00"
             :rev 1
             :val {:movie/title "Interstellar"
                   :movie/year 2014}}])))

  (testing "Records dataspex-specific audit meta"
    (is (= (-> (inspector/inspect-val
                nil (with-meta data
                      {:dataspex.audit/summary "Made some changes"
                       :dataspex.audit/details "Here are the details"})
                apr16-1620)
               :history)
           [{:created-at #inst "2025-04-16T16:20:07.000-00:00"
             :rev 1
             :val {:movie/title "Interstellar"
                   :movie/year 2014}
             :dataspex.audit/summary "Made some changes"
             :dataspex.audit/details "Here are the details"}])))

  (testing "Truncates history"
    (is (= (-> (inspector/inspect-val nil data apr16-1620)
               (inspector/inspect-val (dissoc data :movie/year) apr16-1621)
               (inspector/inspect-val data apr16-1622)
               (inspector/inspect-val (dissoc data :movie/year) apr16-1625)
               :history
               count)
           3)))

  (testing "Optionally does not track history"
    (is (nil? (:history (inspector/inspect-val nil data {:track-changes? false})))))

  (testing "Updates rev without tracking history"
    (is (= (-> (inspector/inspect-val nil data {:track-changes? false})
               (inspector/inspect-val (assoc data :update "Ok!") {:track-changes? false})
               :rev)
           2))))

(def app-store (atom {:my "Data"}))

(deftest inspect-test
  (testing "Tracks history by default"
    (is (= (let [store (atom {})]
             (with-redefs [inspector/now (constantly #inst "2025-04-16T16:19:58")]
               (inspector/inspect store "Store" {:my "Data"}))
             @store)
           {"Store"
            {:dataspex/inspectee "Store"
             :dataspex/path []
             :dataspex/activity :dataspex.activity/browse
             :rev 1
             :val {:my "Data"}
             :history [{:created-at #inst "2025-04-16T16:19:58"
                        :rev 1
                        :val {:my "Data"}}]}})))

  (testing "Inspects atom"
    (is (= (let [dataspex-store (atom {})]
             (with-redefs [inspector/now (constantly #inst "2025-04-16T16:19:58")]
               (inspector/inspect dataspex-store "Store" app-store))
             @dataspex-store)
           {"Store"
            {:dataspex/inspectee "Store"
             :dataspex/path []
             :dataspex/activity :dataspex.activity/browse
             :rev 1
             :val {:my "Data"}
             :subscription :dataspex.inspector/inspect
             :ref app-store
             :history [{:created-at #inst "2025-04-16T16:19:58"
                        :rev 1
                        :val {:my "Data"}}]}})))

  (testing "Inspect watches atom for updates"
    (is (= (let [my-store (atom {})
                 dataspex-store (atom {})]
             (with-redefs [inspector/now (constantly #inst "2025-04-16T16:19:58")]
               (inspector/inspect dataspex-store "Store" my-store))
             (with-redefs [inspector/now (constantly #inst "2025-04-16T17:02:23")]
               (swap! my-store assoc :new "Data"))
             (get-in @dataspex-store ["Store" :history]))
           [{:created-at #inst "2025-04-16T17:02:23"
             :rev 2
             :val {:new "Data"}
             :diff [[[] :+ {:new "Data"}]]}
            {:created-at #inst "2025-04-16T16:19:58"
             :rev 1
             :val {}}])))

  (testing "Uninspect stops watching atom for updates"
    (is (nil? (let [my-store (atom {})
                    dataspex-store (atom {})]
                (with-redefs [inspector/now (constantly #inst "2025-04-16T16:19:58")]
                  (inspector/inspect dataspex-store "App data" my-store))
                (inspector/uninspect dataspex-store "App data")
                (with-redefs [inspector/now (constantly #inst "2025-04-16T17:02:23")]
                  (swap! my-store assoc :new "Data"))
                (get @dataspex-store "App data")))))

  (testing "Inspects Datascript conn"
    (is (= (let [conn (d/create-conn {:person/id {:db/unique :db.unique/identity}})
                 dataspex-store (atom {})]
             (d/transact! conn [{:person/id "christian"
                                 :person/given-name "Christian"}])
             (with-redefs [inspector/now (constantly #inst "2025-04-16T16:19:58")]
               (inspector/inspect dataspex-store "DB" conn))
             (with-redefs [inspector/now (constantly #inst "2025-04-16T17:02:23")]
               (d/transact! conn [{:person/id "christian"
                                   :person/given-name "Christian"
                                   :person/family-name "Johansen"}]))
             (-> @dataspex-store
                 (get-in ["DB" :history])
                 vec
                 (update-in [0 :diff] h/undatom-diff)
                 (update-in [0 :val] (comp h/undatom :eavt))
                 (update-in [1 :val] (comp h/undatom :eavt))))
           [{:created-at #inst "2025-04-16T17:02:23.000-00:00"
             :rev 2
             :val [[1 :person/family-name "Johansen" 536870914 true]
                   [1 :person/given-name "Christian" 536870913 true]
                   [1 :person/id "christian" 536870913 true]]
             :diff [[[0] :+ [1 :person/family-name "Johansen" 536870914 true]]]}
            {:created-at #inst "2025-04-16T16:19:58.000-00:00"
             :rev 1
             :val [[1 :person/given-name "Christian" 536870913 true]
                   [1 :person/id "christian" 536870913 true]]}])))

  (testing "Extracts audit metadata from Datascript transaction"
    (is (= (let [conn (d/create-conn {:person/id {:db/unique :db.unique/identity}})
                 dataspex-store (atom {})]
             (with-redefs [inspector/now (constantly #inst "2025-04-16T16:19:58")]
               (inspector/inspect dataspex-store "DB" conn))
             (with-redefs [inspector/now (constantly #inst "2025-04-16T17:02:23")]
               (d/transact! conn [{:person/id "christian"
                                   :person/given-name "Christian"}
                                  {:db/id :db/current-tx
                                   :dataspex.audit/summary [:db/transact [:person/id "christian"]]}]))
             (-> @dataspex-store
                 (get-in ["DB" :history])
                 first
                 :dataspex.audit/summary))
           [:db/transact [:person/id "christian"]])))

  (testing "Extracts audit metadata from Datascript transaction"
    (is (= (let [dataspex-store (atom {:dataspex/host-str "My machine"})]
             (inspector/inspect dataspex-store "DB" {})
             (-> @dataspex-store
                 (get-in ["DB" :dataspex/host-str])))
           "My machine"))))
