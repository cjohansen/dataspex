(ns dataspex.views-test
  (:require [clojure.test :refer [deftest is testing]]
            [dataspex.inspector :as inspector]
            [dataspex.time :as time]
            [dataspex.views :as views]))

(deftest get-view-options
  (testing "Gets initial view options"
    (is (= (views/get-view-options {} "Store")
           {:dataspex/inspectee "Store"})))

  (testing "Gets view options with current path"
    (is (= (views/get-view-options
            {"Store" {:dataspex/path [:users]}}
            "Store")
           {:dataspex/path [:users]
            :dataspex/inspectee "Store"})))

  (testing "Gets view options with with pagination, sorting, folding, and string renderers"
    (is (= (views/get-view-options
            {"Store" {:dataspex/path [:users]
                      :dataspex/pagination {[:libs] {:offset 2}}
                      :dataspex/sorting {[:users] {:key :user/id}}
                      :dataspex/folding {[:users :user/friends] {:folded? false}}}}
            "Store")
           {:dataspex/inspectee "Store"
            :dataspex/path [:users]
            :dataspex/pagination {[:libs] {:offset 2}}
            :dataspex/sorting {[:users] {:key :user/id}}
            :dataspex/folding {[:users :user/friends] {:folded? false}}})))

  (testing "Ignores technical data"
    (is (= (let [dataspex-store (atom {})]
             (with-redefs [time/now (constantly #inst "2025-04-16T16:19:58")]
               (inspector/inspect dataspex-store "Store" (atom {})))
             (views/get-view-options @dataspex-store "Store"))
           {:dataspex/inspectee "Store"
            :dataspex/path []
            :dataspex/activity :dataspex.activity/browse}))))
