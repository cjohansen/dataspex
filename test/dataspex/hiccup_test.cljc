(ns dataspex.hiccup-test
  (:require [dataspex.helper :as h]
            [dataspex.ui :as-alias ui]
            [clojure.test :refer [deftest is testing]]))

(deftest render-inline-test
  (testing "Renders string"
    (is (= (h/render-inline "String")
           [::ui/string "String"])))

  (testing "Renders keyword"
    (is (= (h/render-inline :keyword)
           [::ui/keyword :keyword])))

  (testing "Renders number"
    (is (= (h/render-inline 42)
           [::ui/number 42])))

  (testing "Renders boolean"
    (is (= (h/render-inline true)
           [::ui/boolean true])))

  (testing "Renders symbol"
    (is (= (h/render-inline 'hello)
           [::ui/symbol 'hello])))

  (testing "Renders nil"
    (is (= (h/render-inline nil)
           [::ui/code "nil"])))

  (testing "Renders UUID"
    (is (= (h/render-inline #uuid "ebb5b6b2-c3c0-4b44-b8c4-bcdfa4a3c906")
           [::ui/literal {::ui/prefix "#uuid"}
            [::ui/string "ebb5b6b2-c3c0-4b44-b8c4-bcdfa4a3c906"]])))

  (testing "Renders insts"
    (is (= (h/render-inline #inst "2025-04-05")
           [::ui/literal {::ui/prefix "#inst"}
            [::ui/string "2025-04-05T00:00:00.000-00:00"]])))

  (testing "Renders short vector"
    (is (= (h/render-inline [:hello])
           [::ui/vector
            [::ui/keyword :hello]])))

  (testing "Renders longer vector"
    (is (= (h/render-inline
            [:hello :hello :hello :hello :hello
             :hello :hello :hello :hello :hello
             :hello :hello :hello :hello :hello
             :hello :hello :hello :hello])
           [::ui/link "[19 keywords]"])))

  (testing "Renders longer vector of mixed contents"
    (is (= (h/render-inline
            [:hello :hello :hello :hello :hello
             :hello :hello :hello :hello 2
             :hello :hello :hello :hello :hello
             :hello :hello :hello :hello])
           [::ui/link "[19 items]"])))

  (testing "Renders short list"
    (is (= (h/render-inline '(:hello))
           [::ui/list
            [::ui/keyword :hello]])))

  (testing "Renders longer list"
    (is (= (h/render-inline
            (list
             :hello :hello :hello :hello :hello
             :hello :hello :hello :hello :hello
             :hello :hello :hello :hello :hello
             :hello :hello :hello :hello))
           [::ui/link "(19 keywords)"])))

  (testing "Renders longer list of mixed contents"
    (is (= (h/render-inline
            (list
             :hello :hello :hello :hello :hello
             :hello :hello :hello :hello 2
             :hello :hello :hello :hello :hello
             :hello :hello :hello :hello))
           [::ui/link "(19 items)"])))

  (testing "Renders short seq"
    (is (= (h/render-inline (range 0 5))
           [::ui/list
            [::ui/number 0]
            [::ui/number 1]
            [::ui/number 2]
            [::ui/number 3]
            [::ui/number 4]])))

  (testing "Renders longer seq"
    (is (= (h/render-inline (range 0 50))
           [::ui/link "(50 numbers)"])))

  (testing "Renders longer seq of mixed contents"
    (is (= (h/render-inline (concat (range 0 50) (repeat 3 :lol)))
           [::ui/link "(53 items)"])))

  (testing "Renders indefinite seq"
    (is (= (h/render-inline (range))
           [::ui/link "(1000+ items)"])))

  (testing "Renders short set"
    (is (= (h/render-inline #{:hello})
           [::ui/set
            [::ui/keyword :hello]])))

  (testing "Renders longer set"
    (is (= (h/render-inline
            #{:hello :hallo :hillo :hollo :hullo
              :hella :halla :hilla :holla :hulla
              :helle :halle :hille :holle :hulle
              :hellu :hallu :hillu :hollu})
           [::ui/link "#{19 keywords}"])))

  (testing "Renders longer set of mixed contents"
    (is (= (h/render-inline
            (set (conj (range 0 50) :lol)))
           [::ui/link "#{51 items}"])))

  #?(:cljs
     (testing "Renders short JS array"
       (is (= (h/render-inline #js ["hello"])
              [::ui/vector
               {:dataspex.ui/prefix "#js"}
               [::ui/string "hello"]])))))
