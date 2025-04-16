(ns dataspex.table-scenes
  (:require [dataspex.icons :as icons]
            [dataspex.ui :as ui]
            [portfolio.replicant :as portfolio :refer-macros [defscene]]))

(portfolio/configure-scenes
 {:title "Table UI elements"
  :idx 20})

(def copy-button
  [ui/button {::ui/title "Copy"
              ::ui/actions []}
   [icons/copy]])

(defscene table
  [ui/table
   [ui/thead
    [ui/th {::ui/actions []}
     [ui/keyword :point/label]
     [icons/sort-ascending]]
    [ui/th {::ui/actions []}
     [ui/keyword :point/latitude]]
    [ui/th {::ui/actions []}
     [ui/keyword :point/longitude]]
    [:th]]

   [ui/tbody
    [ui/tr {::ui/actions []}
     [ui/string "Bulbasaur"]
     [ui/number 37.807962]
     [ui/number -122.475238]
     copy-button]

    [ui/tr {::ui/actions []}
     [ui/string "Charmander"]
     [ui/number 34.062759]
     [ui/number -118.35718]
     copy-button]

    [ui/tr {::ui/actions []}
     [ui/string "Squirtle"]
     [ui/number 37.805929]
     [ui/number -122.429582]
     copy-button]

    [ui/tr {::ui/actions []}
     [ui/string "Magnemite"]
     [ui/number 37.8269775]
     [ui/number -122.425144]
     copy-button]

    [ui/tr {::ui/actions []}
     [ui/string "Magmar"]
     [ui/number 37.571414]
     [ui/number -122.00004]
     copy-button]]])
