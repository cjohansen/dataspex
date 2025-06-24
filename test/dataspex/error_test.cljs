(ns dataspex.error-test
  (:require [cljs.test :refer [deftest testing is]]
            [dataspex.data :as data]
            [dataspex.error :as error]
            [dataspex.helper :as h]
            [dataspex.ui :as-alias ui]
            [lookup.core :as lookup]))

::error/keep

(def nested-ex
  (ex-info
   "Wrapper" {:more-details 12}
   (ex-info "Catastrophic error" {:details 42})))

(deftest exception-test
  (testing "Renders ExceptionInfo inline"
    (is (= (-> (h/render-inline (ex-info "Boom!" {})))
           [::ui/string {::ui/prefix "ExceptionInfo"} "Boom!"])))

  (testing "Renders Error inline"
    (is (= (-> (h/render-inline (js/Error. "Dang")))
           [::ui/string {::ui/prefix "Error"} "Dang"])))

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
