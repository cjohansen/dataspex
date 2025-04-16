(ns dataspex.views-test
  (:require [dataspex.views :as views]
            [clojure.test :refer [deftest is testing]]))

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
            :dataspex/folding {[:users :user/friends] {:folded? false}}}))))
