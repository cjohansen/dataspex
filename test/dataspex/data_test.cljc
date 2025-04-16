(ns dataspex.data-test
  (:require [clojure.core.protocols :as p]
            [clojure.test :refer [deftest is testing]]
            [dataspex.data :as data]
            [dataspex.protocols :as dp]
            [dataspex.views :as views]))

(def datafiable-inline-renderable
  (reify
    p/Datafiable
    (datafy [_]
      "Datafied!")

    dp/IRenderInline
    (render-inline [_ _]
      "Inline rendered!")))

(data/add-string-inspector!
 (fn [s]
   (when (= "Special string" s)
     datafiable-inline-renderable)))

(deftest inspect
  (testing "Returns data when it implements no relevant protocols"
    (is (= (data/inspect {:map? true} {})
           {:map? true})))

  (testing "Returns data implementing IRenderInline when view is inline"
    (is (= (-> datafiable-inline-renderable
               (data/inspect {:dataspex/view views/inline}))
           datafiable-inline-renderable)))

  (testing "Returns datafied when data doesn't implement view protocol"
    (is (= (-> datafiable-inline-renderable
               (data/inspect {:dataspex/view views/dictionary}))
           "Datafied!")))

  (testing "Runs string through custom inspectors, and tests the result for protocol implementations"
    (is (= (data/inspect "Special string" {:dataspex/view views/dictionary})
           "Datafied!"))))
