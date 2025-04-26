(ns dataspex.ui-scenes
  (:require [dataspex.icons :as icons]
            [dataspex.ui :as ui]
            [portfolio.replicant :as portfolio :refer-macros [defscene]]))

(portfolio/configure-scenes
 {:title "Utility UI elements"
  :idx 40})

(defscene link
  [ui/link {::ui/actions []} "[3 strings]"])

(defscene button
  [ui/button {::ui/title "Copy to clipboard"
              ::ui/actions []}
   "Copy"])

(defscene button-with-icon
  [ui/button {::ui/title "Copy to clipboard"
              ::ui/actions []}
   [icons/copy]
   "Copy"])

(defscene icon-button
  [ui/button {::ui/title "Copy to clipboard"
              ::ui/actions []}
   [icons/copy]])

(defscene selected-button
  [ui/button {::ui/title "Copy to clipboard"
              ::ui/selected? true}
   [icons/copy]])

(defscene tag
  [ui/tag
   [ui/keyword :myapp.ui/media]])

(defscene tag-error
  [ui/tag {:data-color "error"}
   [ui/keyword :myapp.ui/media]])

(defscene alert-error
  [ui/alert {:data-color "error"}
   [:h2.h2 "Couldn't load " [ui/keyword :myapp.ui/media]]
   [:p "There was a problem loading the alias. If it is a global alias (e.g.
   defined with defalias), make sure to require the namespace. If it's
   not globally defined, pass the definition to dataspex."]])

(defscene enumeration
  [ui/enumeration
   [ui/link "All (3)"]
   [:span.clickable [ui/keyword :user/id] [::ui/code " (3)"]]
   [:span.clickable [ui/keyword :movie/id] [::ui/code " (5)"]]
   [:span.clickable [ui/keyword :review/id] [::ui/code " (2)"]]])

(defscene path
  [ui/path
   [ui/crumb {::ui/actions []} "."]
   [ui/crumb {::ui/actions []} [ui/keyword :token]]
   [ui/crumb {::ui/actions []} [ui/keyword :data]]
   [ui/crumb [ui/keyword :email]]])

(defscene browser
  [:div
   [ui/navbar
    [ui/path
     [ui/crumb {::ui/actions []} "."]
     [ui/crumb {::ui/actions []} [ui/keyword :token]]
     [ui/crumb {::ui/actions []} [ui/keyword :data]]
     [ui/crumb [ui/keyword :email]]]

    [ui/button-bar
     [ui/button {::ui/title "Viewing in data browser"
                 ::ui/selected? true}
      [icons/browser]]
     [ui/button {::ui/title "View as table"
                 ::ui/actions []}
      [icons/table]]
     [ui/button {::ui/title "View raw data"
                 ::ui/actions []}
      [icons/brackets-curly]]]]
   [ui/dictionary
    [ui/entry
     [ui/symbol 'Type]
     [ui/symbol 'String]]
    [ui/entry
     [ui/symbol 'Value]
     [ui/string "christian@cjohansen.no"]]]])

(defscene toolbar
  [ui/toolbar
   [ui/tabs
    [ui/tab
     {::ui/selected? true}
     "Browse"]
    [ui/tab
     {::ui/actions []}
     "Audit"]]
   [:h2 [:strong "Store"] [:span.subtle.ml-4 "localhost:9090 Chrome macOS"]]
   [ui/button-bar
    [ui/button {::ui/title "Minimize"
                ::ui/actions []}
     [icons/arrows-in-simple]]
    [ui/button {::ui/title "Close"
                ::ui/actions []}
     [icons/x]]]])

(defscene toolbar-minimized
  [ui/toolbar
   [ui/tabs]
   [:h2 [:strong "Store"] [:span.subtle.ml-4 "localhost:9090 Chrome macOS"]]
   [ui/button-bar
    [ui/button {::ui/title "Maximize"
                ::ui/actions []}
     [icons/arrows-out-simple]]
    [ui/button {::ui/title "Close"
                ::ui/actions []}
     [icons/x]]]])

(def toolbar
  [ui/toolbar
   [ui/tabs
    [ui/tab
     {::ui/selected? true}
     "Browse"]
    [ui/tab
     {::ui/actions []}
     "Audit"]]
   [:h2 [:strong "Store"] [:span.subtle.ml-4 "localhost:9090 Chrome macOS"]]
   [ui/button-bar
    [ui/button {::ui/title "Minimize"
                ::ui/actions []}
     [icons/arrows-in-simple]]
    [ui/button {::ui/title "Close"
                ::ui/actions []}
     [icons/x]]]])

(def navbar
  [ui/navbar
   [ui/path
    [ui/crumb {::ui/actions []} "."]
    [ui/crumb {::ui/actions []} [ui/keyword :token]]
    [ui/crumb {::ui/actions []} [ui/keyword :data]]
    [ui/crumb [ui/keyword :email]]]

   [ui/button-bar
    [ui/button {::ui/title "Viewing in data browser"
                ::ui/selected? true}
     [icons/browser]]
    [ui/button {::ui/title "View as table"
                ::ui/actions []}
     [icons/table]]
    [ui/button {::ui/title "View raw data"
                ::ui/actions []}
     [icons/brackets-round]]]])

(def dictionary
  [ui/dictionary
   [ui/entry
    [ui/symbol 'Type]
    [ui/symbol 'String]
    ]
   [ui/entry
    [ui/symbol 'Value]
    [ui/string "christian@cjohansen.no"]
    ]])

(defn render-inspector [& [attrs]]
  [:div.panel attrs
   toolbar
   [:div navbar dictionary]])

(defscene inspector-panel
  render-inspector)

(defscene inspector-panel-small
  (render-inspector {:class :text-sm}))

(defscene inspector-panel-xsmall
  (render-inspector {:class :text-xs}))

(defscene inspector-with-pagination
  [:div.panel
   toolbar
   [:div
    navbar
    [ui/navbar {:class :center}
     [ui/button {::ui/actions []}
      [icons/caret-left]]
     [:span.code.text-smaller.subtle "100-199 of 479"]
     [ui/button {::ui/actions []}
      [icons/caret-right]]]
    dictionary]])
