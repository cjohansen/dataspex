(ns dataspex.exception-test
  (:require [clojure.test :refer [deftest is testing]]
            [dataspex.data :as data]
            [dataspex.exception :as exception]
            [dataspex.helper :as h]
            [dataspex.ui :as-alias ui]
            [lookup.core :as lookup]))

::exception/keep

(def nested-ex
  (ex-info
   "Wrapper" {:more-details 12}
   (ex-info "Catastrophic error" {:details 42})))

(deftest exception-test
  (testing "Renders ExceptionInfo inline"
    (is (= (-> (h/render-inline (ex-info "Boom!" {})))
           [::ui/string {::ui/prefix "ExceptionInfo"} "Boom!"])))

  (testing "Renders Exception inline"
    (is (= (-> (h/render-inline (Exception. "Dang")))
           [::ui/string {::ui/prefix "Exception"} "Dang"])))

  (testing "Renders exception dictionary"
    (is (= (->> (ex-info "Catastrophic error" {:details 42})
                h/render-dictionary
                (lookup/select ::ui/entry)
                (map (comp lookup/text first lookup/children)))
           ["Type" ":message" ":stacktrace"])))

  (testing "Renders dictionary with cause"
    (is (= (->> nested-ex
                h/render-dictionary
                (lookup/select ::ui/entry)
                (map (comp lookup/text first lookup/children)))
           ["Type" ":message" ":stacktrace" ":cause"])))

  (testing "Renders cause"
    (is (= (->> (data/nav-in nested-ex [:cause])
                h/render-dictionary
                (lookup/select ::ui/entry)
                (map (comp lookup/text first lookup/children)))
           ["Type" ":message" ":stacktrace"]))))
