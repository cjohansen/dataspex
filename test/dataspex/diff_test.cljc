(ns dataspex.diff-test
  (:require [clojure.test :refer [deftest testing is]]
            [dataspex.diff :as diff]))

(deftest diff-test
  (testing "Includes removed value in deletions"
    (is (= (diff/diff
            {:name "Dataspex" :version "2025-04-17"}
            {:name "Dataspex"})
           [[[:version] :- "2025-04-17"]])))

  (testing "Full replacement of empty value is just considered insertion"
    (is (= (diff/diff
            {}
            {:name "Dataspex"})
           [[[] :+ {:name "Dataspex"}]]))))

(deftest get-diff-stats-test
  (testing "Counts operations"
    (is (= (->> (diff/diff
                 {:name "Dataspex" :version "2025-04-17"}
                 {:name "Dataspex" :sha "a9012bc732" :ref "HEAD"})
                diff/get-stats)
           {:insertions 2
            :deletions 1})))

  (testing "Counts replacements as +/- 1"
    (is (= (->> (diff/diff
                 {:name "Dataspex" :version "2025-04-17"}
                 {:name "Dataspex" :version "2025-04-18"})
                diff/get-stats)
           {:insertions 1
            :deletions 1}))))

(deftest summarize-diffs-test
  (testing "single insertion"
    (is (= (->> (diff/diff
                 {:name "Dataspex"}
                 {:name "Dataspex" :version "2025-04-17"})
                diff/summarize)
           [{:path [:version]
             :insertions 1}])))

  (testing "single removal"
    (is (= (->> (diff/diff
                 {:name "Dataspex" :version "2025-04-17"}
                 {:name "Dataspex"})
                diff/summarize)
           [{:path [:version]
             :deletions 1}])))

  (testing "single insertion and single removal, different keys"
    (is (= (->> (diff/diff
                 {:name "Dataspex" :version "2025-04-17"}
                 {:name "Dataspex" :sha "a9012bc732"})
                diff/summarize)
           [{:path [:version]
             :deletions 1}
            {:path [:sha]
             :insertions 1}])))

  (testing "replacement"
    (is (= (->> (diff/diff
                 {:name "Dataspex" :sha "89027cbf23"}
                 {:name "Dataspex" :sha "a9012bc732"})
                diff/summarize)
           [{:path [:sha]
             :insertions 1
             :deletions 1}])))

  (testing "insertion and removal on same nested map"
    (is (= (->> (diff/diff
                 {:lib {:name "Dataspex" :version "2025-04-17"}}
                 {:lib {:name "Dataspex" :sha "a9012bc732"}})
                diff/summarize)
           [{:path [:lib]
             :insertions 1
             :deletions 1}]))))
