(ns dataspex.inspector-test
  (:require [clojure.test :refer [deftest is testing]]
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

  (testing "Tracks diffs between versions"
    (is (= (-> (inspector/inspect-val nil data apr16-1620)
               (inspector/inspect-val (dissoc data :movie/year) apr16-1621)
               :history)
           [{:created-at #inst "2025-04-16T16:21:14.000-00:00"
             :rev 2
             :val {:movie/title "Interstellar"}
             :diff [[[:movie/year] :-]]}
            {:created-at #inst "2025-04-16T16:20:07.000-00:00"
             :rev 1
             :val {:movie/title "Interstellar"
                   :movie/year 2014}}])))

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
