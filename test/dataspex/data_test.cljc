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

(deftest nav-in
  (testing "Navigates in string"
    (is (= (data/nav-in "String" []) "String"))
    (is (= (data/nav-in "String" [:k]) nil)))

  (testing "Navigates in keyword"
    (is (= (data/nav-in :kw []) :kw))
    (is (= (data/nav-in :kw [:k]) nil)))

  (testing "Navigates in symbol"
    (is (= (data/nav-in 'sym []) 'sym))
    (is (= (data/nav-in 'sym [:k]) nil)))

  (testing "Navigates in number"
    (is (= (data/nav-in 42 []) 42))
    (is (= (data/nav-in 42 [:k]) nil)))

  (testing "Navigates in boolean"
    (is (= (data/nav-in true []) true))
    (is (= (data/nav-in true [:k]) nil)))

  (testing "Navigates in vector"
    (is (= (data/nav-in [:a :b :c] []) [:a :b :c]))
    (is (= (data/nav-in [:a :b :c] [1]) :b))
    (is (= (data/nav-in [:a :b :c] [:k]) nil)))

  (testing "Navigates in list"
    (is (= (data/nav-in '(:a :b :c) []) '(:a :b :c)))
    (is (= (data/nav-in '(:a :b :c) [1]) :b))
    (is (= (data/nav-in '(:a :b :c) [:k]) nil)))

  (testing "Navigates in seq"
    (is (= (data/nav-in (map identity [:a :b :c]) []) '(:a :b :c)))
    (is (= (data/nav-in (map identity [:a :b :c]) [1]) :b))
    (is (= (data/nav-in (map identity [:a :b :c]) [:k]) nil)))

  (testing "Navigates in set"
    (is (= (data/nav-in #{:a :b :c} []) #{:a :b :c}))
    (is (= (data/nav-in #{:a :b :c} [:a]) :a))
    (is (= (data/nav-in #{:a :b :c} [1]) nil)))

  (testing "Navigates through nested data"
    (is (= (-> {:people
                (for [[name langs] [["Ada" [:mathematical-notation :english]]
                                    ["Christian" [:clojure :norwegian]]]]
                  {:person/name name
                   :person/languages langs})}
               (data/nav-in [:people 0 :person/languages 1])) :english)))

  #?(:cljs
     (testing "Navigates in date"
       (is (= (data/nav-in (js/Date. 2025 0 1 12 0 0 0) []) (js/Date. 2025 0 1 12 0 0 0)))
       (is (= (data/nav-in (js/Date. 2025 0 1 12 0 0 0) [:year]) 2025))))

  #?(:cljs
     (testing "Navigates in JS array"
       (let [arr #js ["a" "b" "c"]]
         (is (= (data/nav-in arr []) arr))
         (is (= (data/nav-in arr [1]) "b"))
         (is (= (data/nav-in arr [:k]) nil)))))

  #?(:cljs
     (testing "Navigates in JS object"
       (let [obj #js {:a 1, :b 2 :c 3}]
         (is (= (data/nav-in obj []) obj))
         (is (= (data/nav-in obj [:b]) 2))
         (is (= (data/nav-in obj [2]) nil))))))
