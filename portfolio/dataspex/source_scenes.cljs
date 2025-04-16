(ns dataspex.source-scenes
  (:require [dataspex.ui :as ui]
            [portfolio.replicant :as portfolio :refer-macros [defscene]]))

(portfolio/configure-scenes
 {:title "Source listing scenes"
  :idx 30})

(defscene string-source
  [ui/source
   [ui/string {::ui/actions []}
    "Hello!"]])

(defscene keyword-source
  [ui/source
   [ui/keyword {::ui/actions []}
    :person/id]])

(defscene boolean-source
  [ui/source
   [ui/boolean {::ui/actions []}
    true]])

(defscene symbol-source
  [ui/source
   [ui/symbol {::ui/actions []}
    'some/symbol]])

(defscene literal-source
  [ui/source
   [ui/literal {::ui/actions []
                ::ui/prefix "#inst"}
    [ui/string "2025-04-09T12:59:13.000Z"]]])

(defscene tuple-source
  [ui/source
   [ui/inline-tuple {::ui/prefix "datom"}
    [ui/number {::ui/actions []} 124]
    [ui/keyword :user/id]
    [ui/literal {::ui/prefix "#uuid"}
     [ui/string "941291f5-6661-476d-b839-7d457be329e5"]]]])

(defscene collection-source
  [ui/source
   [ui/map
    [::ui/map-entry
     [ui/keyword {::ui/actions []} :user/id]
     [ui/number 42]]
    [::ui/map-entry
     [ui/keyword {::ui/actions []} :user/name]
     [ui/string "Ada Lovelace"]]
    [::ui/map-entry
     [ui/keyword {::ui/actions []} :user/active?]
     [ui/boolean true]]
    [::ui/map-entry
     [ui/keyword {::ui/actions []} :user/settings]
     [ui/map
      [::ui/map-entry
       [ui/keyword {::ui/actions []} :theme]
       [ui/keyword :dark]]
      [::ui/map-entry
       [ui/keyword {::ui/actions []} :language]
       [ui/string "en-GB"]]
      [::ui/map-entry
       [ui/keyword {::ui/actions []} :js-config]
       [ui/map {::ui/prefix "#js"}
        [::ui/map-entry
         [ui/keyword {::ui/actions []} :autosave]
         [ui/boolean true]]
        [::ui/map-entry
         [ui/keyword {::ui/actions []} :fontSize]
         [ui/number 14]]
        [::ui/map-entry
         [ui/keyword {::ui/actions []} :shortcuts]
         [ui/vector {::ui/prefix "#js"}
          [ui/string "ctrl+s"]
          [ui/string "cmd+p"]]]]]]]
    [::ui/map-entry
     [ui/keyword {::ui/actions []} :user/history]
     [ui/vector
      [ui/map
       [::ui/map-entry
        [ui/keyword {::ui/actions []} :action]
        [ui/keyword :login]]
       [::ui/map-entry
        [ui/keyword {::ui/actions []} :timestamp]
        [ui/number 1700000000000]]]
      [ui/map
       [::ui/map-entry
        [ui/keyword {::ui/actions []} :action]
        [ui/keyword :edit]]
       [::ui/map-entry
        [ui/keyword {::ui/actions []} :timestamp]
        [ui/number 1700000001000]]
       [::ui/map-entry
        [ui/keyword {::ui/actions []} :details]
        [ui/vector
         [ui/string "updated"]
         [ui/keyword :email]]]]]]
    [::ui/map-entry
     [ui/keyword {::ui/actions []} :user/tags]
     [ui/set
      [ui/symbol 'scientist]
      [ui/symbol 'engineer]
      [ui/symbol 'pioneer]]]
    [::ui/map-entry
     [ui/keyword {::ui/actions []} :user/notes]
     [ui/list
      [ui/string "Loves mathematics"]
      [ui/string "First programmer"]]]]])

(defscene js-object-source
  :title "JS Object source"
  [ui/source
   [ui/map {::ui/prefix "#js"}
    [::ui/map-entry
     [ui/keyword {::ui/actions []} :user/id]
     [ui/number 42]]
    [::ui/map-entry
     [ui/keyword {::ui/actions []} :user/name]
     [ui/string "Ada Lovelace"]]]])

(defscene hiccup-tag
  [:div
   [:div [ui/hiccup-tag :div#main.media]]
   [:div [ui/hiccup-tag :div.media.m-4]]
   [:div [ui/hiccup-tag :dataspex.ui/keyword.media.m-4]]])

(defscene hiccup-source
  [ui/hiccup
   [ui/vector
    [ui/hiccup-tag :div.media]
    [ui/map
     [::ui/map-entry
      [ui/keyword :on]
      [ui/map
       [::ui/map-entry
        [ui/keyword :click]
        [ui/vector
         [ui/vector
          [ui/keyword :assoc-in]
          [ui/vector
           [ui/keyword :open?]
           [ui/boolean true]]]]]]]]
    [ui/vector
     [ui/hiccup-tag :aside.media-thumb]
     [ui/vector
      [ui/hiccup-tag :img.rounded-lg]
      [ui/map
       [::ui/map-entry
        [ui/keyword :src]
        [ui/string "/images/66ca0a93-fadd-493b-8ea7-83361aa0549b/christian.jpg"]]]]]
    [ui/vector
     [ui/hiccup-tag :main.grow]
     [ui/vector
      [ui/hiccup-tag :h2.font-bold]
      [ui/string "Christian Johansen"]]
     [ui/vector
      [ui/hiccup-tag :p]
      [ui/string "Just wrote some documentation for Replicant."]]
     [ui/vector
      [ui/hiccup-tag :p.opacity-50]
      [ui/string "Posted February 26th 2025"]]]]])

(defscene hiccup-source-with-source-markers
  [ui/hiccup
   [ui/vector
    [ui/hiccup-tag {:data-folded "false"
                    ::ui/actions []}
     :div.media]
    [ui/tag {::ui/inline? true} [ui/keyword :myapp.ui/media]]
    [ui/map
     [::ui/map-entry
      [ui/keyword :on]
      [ui/map
       [::ui/map-entry
        [ui/keyword :click]
        [ui/vector
         [ui/vector
          [ui/keyword :assoc-in]
          [ui/vector
           [ui/keyword :open?]
           [ui/boolean true]]]]]]]]
    [ui/vector
     [ui/hiccup-tag {:data-folded "false"
                     ::ui/actions []}
      :aside.media-thumb]
     [ui/tag {::ui/inline? true
              ::ui/actions []}
      [ui/hiccup-tag :myapp.ui/media-thumb]]
     [ui/vector
      [ui/hiccup-tag :img.rounded-lg]
      [ui/map
       [::ui/map-entry
        [ui/keyword :src]
        [ui/string "/images/66ca0a93-fadd-493b-8ea7-83361aa0549b/christian.jpg"]]]]]
    [ui/vector
     [ui/hiccup-tag {:data-folded "false"
                     ::ui/actions []}
      :main.grow]
     [ui/vector
      [ui/hiccup-tag :h2.font-bold]
      [ui/string "Christian Johansen"]]
     [ui/vector
      [ui/hiccup-tag :p]
      [ui/string "Just wrote some documentation for Replicant."]]
     [ui/vector
      [ui/hiccup-tag :p.opacity-50]
      [ui/string "Posted February 26th 2025"]]
     [ui/vector {::ui/actions []}
      [ui/hiccup-tag {:data-folded "true"} :p]
      [ui/code "..."]]]]])

(defscene hiccup-source-problems
  [ui/hiccup
   [ui/vector
    [ui/hiccup-tag :div.media]
    [ui/tag {::ui/inline? true
             :data-color "error"}
     [ui/keyword :myapp.ui/media]]
    [ui/alert {:data-color "error"}
     [:h2.h2 "Couldn't load " [ui/keyword :myapp.ui/media]]
     [:p "There was a problem loading the alias. If it is a global alias (e.g.
   defined with defalias), make sure to require the namespace. If it's
   not globally defined, pass the definition to dataspex."]]
    [ui/map
     [::ui/map-entry
      [ui/keyword :on]
      [ui/map
       [::ui/map-entry
        [ui/keyword :click]
        [ui/vector
         [ui/vector
          [ui/keyword :assoc-in]
          [ui/vector
           [ui/keyword :open?]
           [ui/boolean true]]]]]]]]
    [ui/vector
     [ui/hiccup-tag {::ui/actions []} :aside.media-thumb]
     [ui/tag {::ui/inline? true}
      [ui/hiccup-tag :myapp.ui/media-thumb]]
     [ui/vector
      [ui/hiccup-tag :img.rounded-lg]
      [ui/map
       [::ui/map-entry
        [ui/keyword :src]
        [ui/string "/images/66ca0a93-fadd-493b-8ea7-83361aa0549b/christian.jpg"]]]]]]])
