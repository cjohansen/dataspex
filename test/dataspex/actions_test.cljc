(ns dataspex.actions-test
  (:require [clojure.test :refer [deftest is testing]]
            [dataspex.actions :as actions]
            [dataspex.helper :as h]))

(deftest assoc-in*-test
  (testing "Assocs a path"
    (is (= (actions/assoc-in* {} [[[:k] "V"]])
           {:k "V"})))

  (testing "Assocs multiple paths"
    (is (= (actions/assoc-in* {} [[[:k] "V"]
                                  [[:o] 12]])
           {:k "V"
            :o 12}))))

(deftest act!-test
  (testing "Updates store once with assoc-in*"
    (is (= (let [store (atom {})
                 res (atom [])]
             (add-watch store ::remember #(swap! res conj %4))
             (actions/act! store
               [[::actions/assoc-in [:path-1] "Val 1"]
                [::actions/assoc-in [:path-2] "Val 2"]])
             @res)
           [{:path-1 "Val 1"
             :path-2 "Val 2"}])))

  (testing "Uninspects value"
    (is (= (let [store (atom {"Store" {:val 42}})]
             (actions/act! store
               [[::actions/uninspect "Store"]])
             @store)
           {}))))

(deftest inspect-revision-test
  (testing "Inspects revision of other value"
    (is (= (-> (actions/plan
                {"Store"
                 {:rev 2
                  :val {:my "New data"}
                  :history [{:created-at #inst "2025-04-16T19:05:23"
                             :rev 2
                             :val {:my "New data"}}
                            {:created-at #inst "2025-04-16T16:19:58"
                             :rev 1
                             :val {:my "Data"}}]}}
                [[::actions/inspect-revision "Store" 1]])
               h/strip-clock-times)
           [[:effect/inspect "Store@HH:mm:ss"
             nil
             {:my "Data"}
             {:auditable? false}]]))))
