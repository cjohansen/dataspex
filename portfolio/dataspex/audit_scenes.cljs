(ns dataspex.audit-scenes
  (:require [dataspex.icons :as icons]
            [dataspex.ui :as ui]
            [portfolio.replicant :as portfolio :refer-macros [defscene]]))

(portfolio/configure-scenes
 {:title "Change tracking elements"
  :idx 35})

(def audit-toolbar
  [ui/toolbar
   [ui/tabs
    [ui/tab [:strong "Store"]]
    [ui/tab {::ui/actions []}
     "Browse"]
    [ui/tab {::ui/selected? true}
     "Audit"]]
   [ui/button-bar
    [ui/button {::ui/title "Minimize"
                ::ui/actions []}
     [icons/arrows-in-simple]]
    [ui/button {::ui/title "Close"
                ::ui/actions []}
     [icons/x]]]])

(def browse-this-version
  [ui/button {::ui/actions []
              ::ui/title "Browse this version"}
   [icons/browser]])

(defn get-revision-header [n & [opt]]
  (let [folded? (get opt :folded? true)
        button (when folded? browse-this-version)
        attrs {:data-folded (str folded?)}]
    (nth
     [[ui/card-header {::ui/actions []}
       [ui/timestamp attrs "21:37:12"]
       [:div.grow
        [ui/success "2"] " insertions, "
        [ui/error "1"] " deletion in 2 keys"]
       button]

      [ui/card-header {::ui/actions []}
       [ui/timestamp attrs "21:36:59"]
       [:div.grow
        [ui/success "2"] " insertions, "
        [ui/error "1"] " deletion in "
        [ui/keyword :command-log]]
       button]

      [ui/card-header
       [ui/timestamp attrs "21:36:37"]
       [:div.grow
        [:strong "1"] " replacement in "
        [ui/vector [ui/keyword :token] [ui/keyword :data]]]
       button]

      [ui/card-header
       [ui/timestamp attrs "21:23:14"]
       [:div.grow
        [ui/success "2"] " insertions, "
        [ui/error "2"] " deletions in "
        [ui/vector
         [ui/keyword :user]
         [ui/keyword :user/settings]]]
       button]]
     n)))

(defscene list-changes
  [:div.panel
   audit-toolbar
   [::ui/card-list.code
    [ui/card (get-revision-header 0)]
    [ui/card (get-revision-header 1)]
    [ui/card (get-revision-header 2)]
    [ui/card (get-revision-header 3)]]])

(defscene list-recent-changes
  [:div.panel
   audit-toolbar
   [::ui/card-list.code
    [ui/card (get-revision-header 0)]
    [ui/card (get-revision-header 1)]
    [ui/card (get-revision-header 2)]
    [ui/card (get-revision-header 3)]
    [ui/card
     [ui/card-body
      [:p "4 older versions have been discarded. Change "
       [ui/keyword :history-limit] " to control how history is truncated."]]]]])

(defscene inspect-changes
  [:div.panel
   audit-toolbar
   [::ui/card-list.code
    [ui/card (get-revision-header 0)]

    [ui/card
     (get-revision-header 1 {:folded? false})
     [ui/card-body
      [:div.diff
       [ui/source
        [ui/vector
         [ui/keyword :command/log]
         [ui/map
          [::ui/map-entry
           [ui/keyword :command/kind]
           [ui/keyword :command/test]]]
         [ui/number 2]]]
       [ui/source {:data-color "success"
                   ::ui/prefix "+"}
        [ui/map
         [::ui/map-entry
          [ui/keyword :status]
          [ui/keyword :command.status/in-flight]]
         [::ui/map-entry
          [ui/keyword :user-time]
          [ui/literal {::ui/prefix "#time/zoned-date-time"}
           [ui/string "2024-10-01T14:49+02:00[Europe/Oslo]"]]]]]]
      [:div
       [ui/button {::ui/actions []
                   ::ui/title "Browse this version"}
        [icons/browser]
        "Browse this version"]]]]

    [ui/card (get-revision-header 2)]
    [ui/card (get-revision-header 3)]]])

(defscene inspect-changes-larger-diff
  [:div.panel
   audit-toolbar
   [::ui/card-list.code
    [ui/card (get-revision-header 0)]
    [ui/card (get-revision-header 1)]
    [ui/card (get-revision-header 2)]

    [ui/card
     (get-revision-header 3 {:folded? false})
     [ui/card-body
      [:article.diff
       [ui/source
        [ui/vector
         [ui/keyword {::ui/actions []} :user]
         [ui/keyword {::ui/actions []} :user/settings]
         [ui/keyword :language]]]
       [ui/source {::ui/prefix "-"
                   :data-color "error"}
        [ui/string "en-GB"]]]
      [:article.diff
       [ui/source
        [ui/vector
         [ui/keyword {::ui/actions []} :user]
         [ui/keyword {::ui/actions []} :user/settings]
         [ui/keyword {::ui/actions []} :config]
         [ui/keyword {::ui/actions []} :autosave]]]
       [ui/source {::ui/prefix "-"
                   :data-color "error"}
        [ui/boolean true]]
       [ui/source {::ui/prefix "+"
                   :data-color "success"}
        [ui/boolean false]]]

      [:article.diff
       [ui/source
        [ui/vector
         [ui/keyword {::ui/actions []} :user]
         [ui/keyword {::ui/actions []} :user/settings]
         [ui/keyword {::ui/actions []} :config]
         [ui/keyword {::ui/actions []} :shortcuts]
         [ui/number 2]]]
       [ui/source {:data-color "success"
                   ::ui/prefix "+"}
        [ui/string "cmd+r"]]]]]]])

(defscene custom-summary
  [:div.panel
   audit-toolbar
   [::ui/card-list.code
    [ui/card
     [ui/card-header {::ui/actions []}
      [ui/timestamp {:data-folded "true"} "21:37:12"]
      [:div.grow.flex
       [ui/vector
        [ui/keyword :actions/transact]
        [ui/vector
         [ui/keyword :user/id]
         [ui/string "cjno"]]]
       [:div.tag
        [ui/success "+2"] " " [ui/error "-1"]]]
      browse-this-version]]

    [ui/card
     [ui/card-header {::ui/actions []}
      [ui/timestamp {:data-folded "true"} "21:36:59"]
      [:div.grow.flex
       [ui/vector
        [ui/keyword :actions/transact]
        [ui/vector
         [ui/keyword :user/id]
         [ui/string "cjno"]]]
       [:div.tag
        [ui/error "-3"]]]
      browse-this-version]]

    [ui/card
     [ui/card-header {::ui/actions []}
      [ui/timestamp {:data-folded "true"} "21:36:37"]
      [:div.grow.flex
       [ui/vector
        [ui/keyword :actions/navigate]
        [ui/map
         [::ui/map-entry
          [ui/keyword :query-params]
          [ui/map
           [::ui/map-entry
            [ui/keyword :user-id]
            [ui/string "cjno"]]]]]]
       [:div.tag
        [ui/success "+1"]]]
      browse-this-version]]

    [ui/card
     [ui/card-header {::ui/actions []}
      [ui/timestamp {:data-folded "true"} "21:23:14"]
      [:div.grow.flex
       [ui/vector
        [ui/keyword :actions/navigate]
        [ui/map
         [::ui/map-entry
          [ui/keyword :page-id]
          [ui/string :pages/user-page]]]]
       [:div.tag
        [ui/success "+4"] " "
        [ui/error "-5"]]]
      browse-this-version]]]])

(defscene no-changes-available
  [:div.panel
   [ui/toolbar
    [ui/tabs
     [ui/tab "Page data"]
     [ui/tab {::ui/actions []}
      "Browse"]
     [ui/tab {::ui/selected? true}
      "Audit"]]
    [ui/button-bar
     [ui/button {::ui/title "Minimize"
                 ::ui/actions []}
      [icons/arrows-in-simple]]
     [ui/button {::ui/title "Close"
                 ::ui/actions []}
      [icons/x]]]]
   [::ui/card-list.code
    [ui/card
     [ui/card-body
      [:p "Page data isn’t currently set up to track changes. To turn it on,
       call inspect with the following options:"]
      [ui/source
       [ui/list
        [ui/symbol 'dataspex/inspect]
        [ui/string "Page data"]
        [ui/symbol 'data]
        [ui/map
         [::ui/map-entry
          [ui/keyword :track-changes?]
          [ui/boolean true]]
         [::ui/map-entry
          [ui/keyword :history-limit]
          [ui/number 25]]]]]]]]])
