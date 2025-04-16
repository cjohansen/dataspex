(ns dataspex.hiccup-test
  (:require [dataspex.helper :as h]
            [dataspex.ui :as-alias ui]
            [clojure.test :refer [deftest is testing]]))

(deftest render-inline-test
  (testing "Renders string"
    (is (= (h/render-inline "String")
           [::ui/string "String"])))

  (testing "Renders keyword"
    (is (= (h/render-inline :keyword)
           [::ui/keyword :keyword])))

  (testing "Renders number"
    (is (= (h/render-inline 42)
           [::ui/number 42])))

  (testing "Renders boolean"
    (is (= (h/render-inline true)
           [::ui/boolean true])))

  (testing "Renders symbol"
    (is (= (h/render-inline 'hello)
           [::ui/symbol 'hello])))

  (testing "Renders nil"
    (is (= (h/render-inline nil)
           [::ui/code "nil"])))

  (testing "Renders UUID"
    (is (= (h/render-inline #uuid "ebb5b6b2-c3c0-4b44-b8c4-bcdfa4a3c906")
           [::ui/literal {::ui/prefix "#uuid"}
            [::ui/string "ebb5b6b2-c3c0-4b44-b8c4-bcdfa4a3c906"]])))

  (testing "Renders insts"
    (is (= (h/render-inline #inst "2025-04-05")
           [::ui/literal {::ui/prefix "#inst"}
            [::ui/string "2025-04-05T00:00:00.000-00:00"]]))))
