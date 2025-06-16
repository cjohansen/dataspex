(ns dataspex.inline-scenes
  (:require [dataspex.ui :as ui]
            [portfolio.replicant :as portfolio :refer-macros [defscene]]))

(portfolio/configure-scenes
 {:title "Inline UI elements"
  :idx 0})

(defscene string
  [ui/string "String!"])

(defscene number
  [ui/number 42])

(defscene keyword
  [:div
   [:div [ui/keyword :hello]]
   [:div [ui/keyword "hello"]]
   [:div [ui/keyword :namespaced/hello]]
   [:div [ui/keyword ":ns" "keyword"]]])

(defscene boolean
  [ui/boolean true])

(defscene symbol
  [:div
   [:div [ui/symbol 'symbolic-things]]
   [:div [ui/symbol 'namespaced/symbolic-things]]])

(defscene nil-value
  :title "Nil"
  [ui/code "nil"])

(defscene literal
  [ui/literal {::ui/prefix "#inst"}
   [ui/string "2025-04-12T12:00:00Z"]])

(defscene arbitrary-object
  [ui/code "java.awt.Point[x=10,y=20]"])

(defscene vector
  [ui/vector
   [ui/keyword :a]
   [ui/keyword :b]
   [ui/keyword :c]])

(defscene set
  [ui/set
   [ui/keyword :a]
   [ui/keyword :b]
   [ui/keyword :c]])

(defscene list
  [ui/list
   [ui/keyword :a]
   [ui/keyword :b]
   [ui/keyword :c]])

(defscene map
  [ui/map
   [::ui/map-entry
    [ui/keyword :name]
    [ui/string "Christian"]]
   [::ui/map-entry
    [ui/keyword :keys]
    [ui/number 2]]])

(defscene map-key-summary
  [ui/map
   [::ui/map-entry [ui/keyword :name]]
   [::ui/map-entry [ui/keyword :keys]]])

(def tuple
  [ui/inline-tuple {::ui/prefix "datom"}
   [ui/number 124]
   [ui/keyword :user/id]
   [ui/literal {::ui/prefix "#uuid"}
    [ui/string "941291f5-6661-476d-b839-7d457be329e5"]]])

(def clickable-tuple
  [ui/inline-tuple {::ui/prefix "datom"}
   [ui/number {::ui/actions []} 124]
   [ui/keyword {::ui/actions []} :user/id]
   [ui/literal {::ui/actions []
                ::ui/prefix "#uuid"}
    [ui/string "941291f5-6661-476d-b839-7d457be329e5"]]])

(defscene prefixed-tuple
  [:div
   tuple
   clickable-tuple])

(defscene linked-tuple
  [ui/inline-tuple
   [ui/keyword {::ui/actions []} :user/id]
   [ui/number {::ui/actions []} 245]])

(defscene js-array
  :title "JS Array"
  [ui/vector {::ui/prefix "#js"}
   [ui/string "a"]
   [ui/string "b"]
   [ui/string "c"]])

(defscene js-object
  :title "JS Object"
  [ui/map {::ui/prefix "#js"}
   [::ui/map-entry
    [ui/keyword :name]
    [ui/string "Christian"]]
   [::ui/map-entry
    [ui/keyword :keys]
    [ui/number 2]]])

(defscene js-date
  :title "JS Date"
  [ui/literal {::ui/prefix "#inst"}
   [ui/string "2025-04-08T20:21:19.046-00:00"]])

(defscene element-as-hiccup
  [ui/hiccup {::ui/prefix "#js/SVGSVGElement"
              ::ui/inline? true}
   [ui/vector
    [ui/hiccup-tag :svg]
    [ui/map {::ui/inline? true}
     [::ui/map-entry [ui/keyword :style]]
     [::ui/map-entry [ui/keyword :viewBox]]
     [::ui/map-entry [ui/keyword :xmlns]]]
    [ui/string "..."]]])
