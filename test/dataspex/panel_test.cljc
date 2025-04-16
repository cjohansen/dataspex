(ns dataspex.panel-test
  (:require [clojure.test :refer [deftest is testing]]
            [dataspex.actions :as-alias actions]
            [dataspex.ui :as-alias ui]
            [dataspex.icons :as-alias icons]
            [dataspex.panel :as panel]))

(deftest render-title-bar
  (testing "Renders title bar"
    (is (= (panel/render-title-bar
            {:dataspex/path ["Store"]
             :dataspex/inspectee "Store"})
           [::ui/toolbar
            [::ui/tabs
             [::ui/tab "Store"]
             [::ui/tab {::ui/selected? true} "Browse"]]
            [::ui/button-bar
             [::ui/button {::ui/title "Minimize"
                           ::ui/actions [[::actions/assoc-in ["Store" :dataspex/render?] false]]}
              [::icons/arrows-in-simple]]
             [::ui/button {::ui/title "Close"
                           ::ui/actions [[::actions/uninspect "Store"]]}
              [::icons/x]]]])))

  (testing "Renders minimized title bar"
    (is (= (panel/render-title-bar
            {:dataspex/inspectee "Store"
             :dataspex/render? false})
           [::ui/toolbar
            [::ui/tabs
             [::ui/tab "Store"]
             [::ui/tab "Browse"]]
            [::ui/button-bar
             [::ui/button {::ui/title "Maximize"
                           ::ui/actions [[::actions/assoc-in ["Store" :dataspex/render?] true]]}
              [::icons/arrows-out-simple]]
             [::ui/button {::ui/title "Close"
                           ::ui/actions [[::actions/uninspect "Store"]]}
              [::icons/x]]]]))))
