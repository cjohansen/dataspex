(ns dataspex.js-test
  (:require [cljs.test :refer [deftest is]]
            [dataspex.data :as data]
            [dataspex.hiccup]))

(deftest js-object-test
  (is (data/js-object? #js {:hah "Lol"}))
  (is (not (data/js-object? #uuid "ebb5b6b2-c3c0-4b44-b8c4-bcdfa4a3c906")))
  (is (not (data/js-object? (js/Date.))))
  (is (not (data/js-object? {})))
  (is (not (data/js-object? [])))
  (is (not (data/js-object? '()))))
