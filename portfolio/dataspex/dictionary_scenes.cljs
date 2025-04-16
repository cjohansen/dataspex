(ns dataspex.dictionary-scenes
  (:require [dataspex.icons :as icons]
            [dataspex.ui :as ui]
            [portfolio.replicant :as portfolio :refer-macros [defscene]]))

(portfolio/configure-scenes
 {:title "Dictionary UI elements"
  :idx 10})

(def copy-button
  [ui/button {::ui/title "Copy"
              ::ui/actions []}
   [icons/copy]])

(defscene dictionary
  [ui/dictionary
   [ui/entry
    [ui/symbol "^meta"]
    [ui/map {::ui/actions []}
     [::ui/map-entry
      [ui/keyword :git-sha]]
     [::ui/map-entry
      [ui/keyword :build-date]]]
    copy-button]

   [ui/entry
    [ui/keyword :name]
    [ui/string "Christian"]
    copy-button]

   [ui/entry
    [ui/keyword :language]
    [ui/string "Clojure"]
    copy-button]

   [ui/entry
    [ui/keyword :environment]
    [ui/string "Browser"]
    copy-button]

   [ui/entry
    [ui/keyword :xs]
    [ui/link {::ui/actions []}
     "[3 maps]"]
    copy-button]])

(defscene datascript-index
  [ui/dictionary {:class :table-auto}
   [ui/tuple {::ui/prefix "datom"}
    [ui/number {::ui/actions []} 1]
    [ui/keyword {::ui/actions []} :user/id]
    [ui/literal {::ui/prefix "#uuid"}
     [ui/string "ea75f3b6-3f91-486b-900d-5e7fcae9fd55"]]
    [ui/number {::ui/actions []} "536870913"]
    [ui/boolean true]]

   [ui/tuple {::ui/prefix "datom"}
    [ui/number {::ui/actions []} 1]
    [ui/keyword {::ui/actions []} :user/name]
    [ui/string "Christian Johansen"]
    [ui/number {::ui/actions []} 536870913]
    [ui/boolean true]]

   [ui/tuple {::ui/prefix "datom"}
    [ui/number {::ui/actions []} 2]
    [ui/keyword {::ui/actions []} :user/id]
    [ui/literal {::ui/prefix "#uuid"}
     [ui/string "86ad3085-bc0b-4cc2-bab9-cd0ba583a58b"]]
    [ui/number {::ui/actions []} 536870914]
    [ui/boolean true]]

   [ui/tuple {::ui/prefix "datom"}
    [ui/number {::ui/actions []} 2]
    [ui/keyword {::ui/actions []} :user/friends]
    [ui/number 1]
    [ui/number {::ui/actions []} 536870914]
    [ui/boolean true]]])
