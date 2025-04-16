(ns dataspex.panel-test
  (:require [clojure.test :refer [deftest is testing]]
            [dataspex.actions :as-alias actions]
            [dataspex.icons :as-alias icons]
            [dataspex.panel :as panel]
            [dataspex.ui :as-alias ui]
            [dataspex.views :as views]
            [lookup.core :as lookup]))

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

(deftest render-view-menu
  (testing "Renders full menu for collection of maps"
    (is (= (panel/render-view-menu [{}] {:dataspex/inspectee "Store"
                                         :dataspex/view views/dictionary
                                         :dataspex/path [:data]})
           [::ui/button-bar
            [::ui/button
             {::ui/title "Viewing in data browser"
              ::ui/selected? true}
             [::icons/browser]]
            [::ui/button
             {::ui/title "View raw data"
              ::ui/actions [[::actions/assoc-in ["Store" :dataspex/view [:data]] views/source]
                            [::actions/assoc-in ["Store" :dataspex/default-view] views/source]]}
             [::icons/brackets-square]]
            [::ui/button
             {::ui/title "View as table"
              ::ui/actions [[::actions/assoc-in ["Store" :dataspex/view [:data]] views/table]
                            [::actions/assoc-in ["Store" :dataspex/default-view] views/table]]}
             [::icons/table]]])))

  (testing "Renders disabled button for unsupported table view"
    (is (= (->> (panel/render-view-menu "String" {})
                lookup/children
                last)
           [::ui/button
            {::ui/title "The data doesn't support the table view"}
            [::icons/table]])))

  (testing "Renders appropriate source icon for map"
    (is (= (->> (panel/render-view-menu {} {})
                lookup/children
                second
                lookup/children
                last)
           [::icons/brackets-curly])))

  (testing "Renders appropriate source icon for list"
    (is (= (->> (panel/render-view-menu '() {})
                lookup/children
                second
                lookup/children
                last)
           [::icons/brackets-round])))

  (testing "Renders appropriate source icon for vector"
    (is (= (->> (panel/render-view-menu [] {})
                lookup/children
                second
                lookup/children
                last)
           [::icons/brackets-square]))))

(deftest render-path
  (testing "Renders root path as a dot"
    (is (= (panel/render-path [] {})
           [::ui/path
            [::ui/crumb {} "."]])))

  (testing "Renders path of one string"
    (is (= (panel/render-path ["users"] {:dataspex/inspectee "Store"})
           [::ui/path
            [::ui/crumb {::ui/actions [[::actions/assoc-in ["Store" :dataspex/path] []]]}
             "."]
            [::ui/crumb
             [::ui/string "users"]]])))

  (testing "Renders path of strings, keywords and numbers"
    (is (= (panel/render-path [:users 0] {:dataspex/inspectee "Store"})

           [::ui/path
            [::ui/crumb
             {::ui/actions [[::actions/assoc-in ["Store" :dataspex/path] []]]}
             "."]
            [::ui/crumb
             {::ui/actions
              [[::actions/assoc-in ["Store" :dataspex/path] [:users]]]}
             [::ui/keyword :users]]
            [::ui/crumb [::ui/number 0]]]))))

(deftest render-pagination-bar
  (testing "Does not paginate strings"
    (is (nil? (panel/render-pagination-bar "String" {}))))

  (testing "Does not paginate collection that fits on one page"
    (is (nil? (panel/render-pagination-bar (range 200) {}))))

  (testing "Can go to next page"
    (is (= (panel/render-pagination-bar
            (range 200)
            {:dataspex/inspectee "App data"
             :dataspex/path [:data]
             :dataspex/pagination
             {[:data] {:page-size 100}}})
           [::ui/navbar.center
            [::ui/button [::icons/caret-left]]
            [:span.code.text-smaller.subtle "0-99 of 200"]
            [::ui/button
             {::ui/actions
              [[::actions/assoc-in ["App data" :dataspex/pagination [:data] :offset] 100]]}
             [::icons/caret-right]]])))

  (testing "Can go to previous page"
    (is (= (panel/render-pagination-bar
            (range 200)
            {:dataspex/inspectee "App data"
             :dataspex/path [:data]
             :dataspex/pagination
             {[:data] {:page-size 100
                       :offset 100}}})
           [::ui/navbar.center
            [::ui/button
             {::ui/actions
              [[::actions/assoc-in ["App data" :dataspex/pagination [:data] :offset] 0]]}
             [::icons/caret-left]]
            [:span.code.text-smaller.subtle "100-199 of 200"]
            [::ui/button [::icons/caret-right]]])))

  (testing "Can go back and forth"
    (is (= (panel/render-pagination-bar
            (range 479)
            {:dataspex/inspectee "xs"
             :dataspex/path [:datas]
             :dataspex/pagination
             {[:datas] {:offset 100
                        :page-size 100}}})
           [::ui/navbar.center
            [::ui/button
             {::ui/actions
              [[::actions/assoc-in ["xs" :dataspex/pagination [:datas] :offset] 0]]}
             [::icons/caret-left]]
            [:span.code.text-smaller.subtle "100-199 of 479"]
            [::ui/button
             {::ui/actions
              [[::actions/assoc-in ["xs" :dataspex/pagination [:datas] :offset] 200]]}
             [::icons/caret-right]]]))))
