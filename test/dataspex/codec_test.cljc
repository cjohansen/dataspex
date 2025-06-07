(ns dataspex.codec-test
  (:require [clojure.test :refer [deftest testing is]]
            [dataspex.codec :as codec]))

(deftest roundtrip-test
  (testing "Roundtrips a map"
    (is (= (codec/parse-string (codec/generate-string {:hello "World"}))
           {:hello "World"})))

  (testing "Roundtrips a unreadable keyword"
    (is (= (codec/parse-string (codec/generate-string {:hello (keyword ":holy" "keyword")}))
           {:hello (keyword ":holy" "keyword")}))))
