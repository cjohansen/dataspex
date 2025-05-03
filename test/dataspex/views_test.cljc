(ns dataspex.views-test
  (:require [clojure.test :refer [deftest is testing]]
            [dataspex.inspector :as inspector]
            [dataspex.time :as time]
            [dataspex.views :as views]))

(deftest get-view-options
  (testing "Gets initial view options"
    (is (= (:opt (views/get-render-data {} "Store"))
           {:dataspex/inspectee "Store"
            :dataspex/view :dataspex.views/dictionary})))

  (testing "Gets view options with current path"
    (is (= (-> {"Store" {:dataspex/path [:users]}}
               (views/get-render-data "Store")
               :opt)
           {:dataspex/path [:users]
            :dataspex/inspectee "Store"
            :dataspex/view :dataspex.views/dictionary})))

  (testing "Gets view options with with pagination, sorting, folding, and string renderers"
    (is (= (-> {"Store" {:dataspex/path [:users]
                         :dataspex/pagination {[:libs] {:offset 2}}
                         :dataspex/sorting {[:users] {:key :user/id}}
                         :dataspex/folding {[:users :user/friends] {:folded? false}}}}
               (views/get-render-data "Store")
               :opt)
           {:dataspex/inspectee "Store"
            :dataspex/path [:users]
            :dataspex/pagination {[:libs] {:offset 2}}
            :dataspex/sorting {[:users] {:key :user/id}}
            :dataspex/folding {[:users :user/friends] {:folded? false}}
            :dataspex/view :dataspex.views/dictionary})))

  (testing "Uses default theme"
    (is (= (-> {"Store" {:dataspex/path [:users]}
                :dataspex/theme :dark}
               (views/get-render-data "Store")
               :opt
               :dataspex/theme)
           :dark)))

  (testing "Ignores technical data"
    (is (= (let [dataspex-store (atom {})]
             (with-redefs [time/now (constantly #inst "2025-04-16T16:19:58")]
               (inspector/inspect dataspex-store "Store" (atom {})))
             (:opt (views/get-render-data @dataspex-store "Store")))
           {:dataspex/inspectee "Store"
            :dataspex/path []
            :dataspex/activity :dataspex.activity/browse
            :dataspex/view :dataspex.views/dictionary}))))

(deftest get-current-view
  (testing "Defaults to dictionary"
    (is (= (views/get-current-view {} {}) views/dictionary)))

  (testing "Uses data's view"
    (is (= (views/get-current-view {}
             {:dataspex/view {[] views/source}
              :dataspex/path []})
           views/source)))

  (testing "Uses default view"
    (is (= (views/get-current-view {}
             {:dataspex/default-view views/source
              :dataspex/path []})
           views/source)))

  (testing "Prefers path specific view over default view"
    (is (= (views/get-current-view {}
             {:dataspex/default-view views/source
              :dataspex/view {[:users] views/dictionary}
              :dataspex/path [:users]})
           views/dictionary)))

  (testing "Does not use default view when not supported"
    (is (= (views/get-current-view {}
             {:dataspex/default-view views/table
              :dataspex/path [:users]})
           views/dictionary)))

  (testing "Defaults to source view for hiccup"
    (is (= (views/get-current-view [:h1 "Hello"]
             {:dataspex/default-view views/table
              :dataspex/path [:users]})
           views/source))))
