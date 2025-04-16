(ns dataspex.ui-test
  (:require [dataspex.ui :as ui]
            [clojure.test :refer [deftest is testing]]))

(deftest content-length-test
  (testing "Calculates length of string"
    (is (= (ui/content-length [ui/string "A string"]) 10))
    (is (= (ui/content-length [ui/string {} "A string"]) 10)))

  (testing "Calculates length of keyword"
    (is (= (ui/content-length [ui/keyword :keyword]) 8)))

  (testing "Calculates length of symbol"
    (is (= (ui/content-length [ui/symbol 'sym]) 3)))

  (testing "Calculates length of boolean"
    (is (= (ui/content-length [ui/boolean true]) 4)))

  (testing "Calculates length of number"
    (is (= (ui/content-length [ui/number 42]) 2)))

  (testing "Calculates length of vector"
    (is (= (ui/content-length
            [ui/vector [ui/number 42] [ui/string "Hello"]])
           12)))

  (testing "Calculates length of list"
    (is (= (ui/content-length
            [ui/list [ui/number 42] [ui/string "Hello"]])
           12)))

  (testing "Calculates length of set"
    (is (= (ui/content-length
            [ui/set [ui/number 42] [ui/string "Hello"]])
           13)))

  (testing "Calculates length of datom"
    (is (= (ui/content-length
            [ui/vector {::ui/prefix "#datom"}
             [ui/number 42]
             [ui/string "Hello"]])
           19)))

  (testing "Calculates length of map"
    (is (= (ui/content-length
            [ui/map
             [::ui/map-entry
              [ui/keyword :name]
              [ui/string "Dataspex"]]
             [::ui/map-entry
              [ui/keyword :version]
              [ui/string "1.0.0"]]])
           36))))
