(ns dataspex.js-test
  (:require [cljs.test :refer [deftest testing is]]
            [cognitect.transit :as t]
            [dataspex.data :as data]
            [dataspex.helper :as h]
            [dataspex.hiccup]
            [dataspex.ui :as ui]))

:dataspex.hiccup/keep

(deftest js-object-test
  (is (data/js-object? #js {:hah "Lol"}))
  (is (not (data/js-object? #uuid "ebb5b6b2-c3c0-4b44-b8c4-bcdfa4a3c906")))
  (is (not (data/js-object? (js/Date.))))
  (is (not (data/js-object? {})))
  (is (not (data/js-object? [])))
  (is (not (data/js-object? '()))))

(defn roundtrip [x]
  (let [w (t/writer :json)
        r (t/reader :json)]
    (t/read r (t/write w x))))

(deftest render-inline-test
  (testing "Renders transit UUIDs as regular UUIDs"
    (is (= (h/render-inline (roundtrip #uuid "d8a1fa84-5e33-4ea9-a68b-3fbd6f365731"))
           [::ui/literal {::ui/prefix "#uuid"}
            [::ui/string "d8a1fa84-5e33-4ea9-a68b-3fbd6f365731"]])))

  (testing "Can render circular objects"
    (is (= (let [o #js {:member #js {:number 42}}
                 obj o]
             (set! (.-inner (.-member obj)) obj)
             (h/render-inline {:dataspex/summarize-above-w -1} o))
           [::ui/map {::ui/prefix "#js"}
            [::ui/map-entry
             [::ui/symbol 'member]
             [::ui/map {::ui/prefix "#js"}
              [::ui/map-entry
               [::ui/symbol 'inner]
               [::ui/symbol "(circular)"]]
              [::ui/map-entry
               [::ui/symbol 'number]
               [::ui/number 42]]]]]))))
