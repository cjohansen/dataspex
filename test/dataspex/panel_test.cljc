(ns dataspex.panel-test
  (:require [clojure.core.protocols :as p]
            [clojure.test :refer [deftest is testing]]
            [dataspex.actions :as-alias actions]
            [dataspex.icons :as-alias icons]
            [dataspex.panel :as panel]
            [dataspex.ui :as-alias ui]
            [dataspex.views :as views]
            [lookup.core :as lookup]))

(deftest render-title-bar
  (testing "Renders title bar"
    (is (= (panel/render-title-bar
            {}
            {:dataspex/path ["Store"]
             :dataspex/inspectee "Store"})
           [::ui/toolbar
            [::ui/tabs
             [::ui/tab {::ui/selected? true} "Browse"]]
            [:h2 [:strong "Store"]]
            [::ui/button-bar
             [::ui/button {::ui/title "Switch to light mode"
                           ::ui/actions [[::actions/assoc-in ["Store" :dataspex/theme] :light]]}
              [::icons/sun]]
             [::ui/button {::ui/title "Minimize"
                           ::ui/actions [[::actions/assoc-in ["Store" :dataspex/render?] false]]}
              [::icons/arrows-in-simple]]
             [::ui/button {::ui/title "Close"
                           ::ui/actions [[::actions/uninspect "Store"]]}
              [::icons/x]]]])))

  (testing "Includes host information"
    (is (= (->> {:dataspex/path ["Store"]
                 :dataspex/inspectee "Store"
                 :dataspex/host-str "localhost:9090 Chrome macOS"}
                (panel/render-title-bar {})
                (lookup/select-one :h2))
           [:h2
            [:strong "Store"]
            [:span {:class #{"ml-4" "subtle"}} "localhost:9090 Chrome macOS"]])))

  (testing "Offers dark mode when currently in light mode"
    (is (= (->> (panel/render-title-bar
                 {}
                 {:dataspex/path ["Store"]
                  :dataspex/inspectee "Store"
                  :dataspex/theme :light})
                (lookup/select-one [::ui/button-bar ":first-child"]))
           [::ui/button
            {::ui/title "Switch to dark mode"
             ::ui/actions
             [[::actions/assoc-in ["Store" :dataspex/theme] :dark]]}
            [::icons/moon]])))

  (testing "Renders audit tab when there are more than one version"
    (is (= (->> (panel/render-title-bar
                 {:history [{} {}]}
                 {:dataspex/path []
                  :dataspex/inspectee "Store"})
                (lookup/select ::ui/tab)
                last)
           [::ui/tab
            {::ui/actions
             [[::actions/assoc-in ["Store" :dataspex/activity] :dataspex.activity/audit]]}
            "Audit"])))

  (testing "Cannot audit value that isn't auditable"
    (is (= (->> (panel/render-title-bar
                 {}
                 {:dataspex/path []
                  :dataspex/inspectee "Store@12:27:06"
                  :dataspex/auditable? false})
                (lookup/select ::ui/tab)
                (map lookup/text))
           ["Browse"])))

  (testing "Selects audit tab when auditing"
    (is (= (->> (panel/render-title-bar
                 {:history [{} {}]}
                 {:dataspex/path []
                  :dataspex/inspectee "Store@12:27:06"
                  :dataspex/activity panel/audit})
                (lookup/select ::ui/tab))
           [[::ui/tab {::ui/actions [[::actions/assoc-in ["Store@12:27:06" :dataspex/activity] panel/browse]]}
             "Browse"]
            [::ui/tab {::ui/selected? true} "Audit"]])))

  (testing "Renders minimized title bar"
    (is (= (panel/render-title-bar
            {}
            {:dataspex/inspectee "Store"
             :dataspex/render? false})
           [::ui/toolbar
            [::ui/tabs]
            [:h2 [:strong "Store"]]
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

(defrecord CustomPathSegment []
  p/Datafiable
  (datafy [_]
    "Ay, caramba!"))

(deftest render-path
  (testing "Renders root path as a dot"
    (is (= (panel/render-path [] {})
           [::ui/path
            [::ui/crumb {} "."]])))

  (testing "Renders path of one string"
    (is (= (panel/render-path ["users"] {:dataspex/inspectee "Store"})
           [::ui/path
            [::ui/crumb {::ui/actions [[::actions/assoc-in ["Store" :dataspex/path] []]
                                       [::actions/assoc-in ["Store" :dataspex/activity] :dataspex.activity/browse]]}
             "."]
            [::ui/crumb
             [::ui/string "users"]]])))

  (testing "Renders path of strings, keywords and numbers"
    (is (= (panel/render-path [:users 0] {:dataspex/inspectee "Store"})
           [::ui/path
            [::ui/crumb
             {::ui/actions [[::actions/assoc-in ["Store" :dataspex/path] []]
                            [::actions/assoc-in ["Store" :dataspex/activity] :dataspex.activity/browse]]}
             "."]
            [::ui/crumb
             {::ui/actions
              [[::actions/assoc-in ["Store" :dataspex/path] [:users]]
               [::actions/assoc-in ["Store" :dataspex/activity] :dataspex.activity/browse]]}
             [::ui/keyword :users]]
            [::ui/crumb [::ui/number 0]]])))

  (testing "Renders path elements inline"
    (is (= (->> (panel/render-path [(->CustomPathSegment)] {})
                (lookup/select ::ui/crumb)
                (mapv lookup/text))
           ["." "Ay, caramba!"])))

  (testing "Truncates long path"
    (is (= (->> (panel/render-path [:reviews :review/id 1001 :review/movie :review/_movie 1002] {})
                (lookup/select ::ui/crumb)
                (mapv lookup/text))
           ["." ":reviews" "..." ":review/movie" ":review/_movie" "1002"]))))

(deftest render-pagination-bar
  (testing "Does not paginate strings"
    (is (nil? (panel/render-pagination-bar "String" {}))))

  (testing "Does not paginate collection that fits on one page"
    (is (nil? (panel/render-pagination-bar (range 100) {}))))

  (testing "Can go to next page"
    (is (= (panel/render-pagination-bar
            {:page-size 100
             :offset 0
             :n 200}
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
            {:page-size 100
             :offset 100
             :n 200}
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
            {:page-size 100
             :offset 100
             :n 479}
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

(deftest render-panel
  (testing "Renders panel in browsing mode by default"
    (is (= (->> (panel/render-panel
                 {"Store" {:dataspex/path []
                           :dataspex/inspectee "Store"
                           :val {:data "Here"}}}
                 "Store")
                (lookup/select [::ui/dictionary ::ui/string]))
           [[::ui/string "Here"]])))

  (testing "Renders hiccup in the source view by default"
    (is (= (->> (panel/render-panel
                 {"Store" {:dataspex/path [:hiccup]
                           :dataspex/inspectee "Store"
                           :val {:hiccup [:h1 "Hello"]}}}
                 "Store")
                (lookup/select [::ui/hiccup])
                count)
           1)))

  (testing "Renders audit log"
    (is (not-empty
         (->> (panel/render-panel
               {"Store" {:dataspex/path []
                         :dataspex/inspectee "Store"
                         :dataspex/activity panel/audit
                         :rev 1
                         :val {}
                         :history [{:rev 1 :val {}}]}}
               "Store")
              (lookup/select ::ui/card-list)))))

  (testing "Renders panel with selected theme"
    (is (= (->> (panel/render-panel
                 {"Store" {:dataspex/path []
                           :dataspex/inspectee "Store"
                           :dataspex/theme :dark}}
                 "Store")
                (lookup/select-one [:div.panel])
                lookup/attrs
                :data-theme)
           "dark"))))
