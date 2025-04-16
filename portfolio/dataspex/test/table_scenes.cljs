(ns dataspex.test.table-scenes
  (:require [dataspex.data :as data]
            [dataspex.hiccup :as hiccup]
            [dataspex.views :as views]
            [portfolio.replicant :as portfolio :refer-macros [defscene]]))

(portfolio/configure-scenes
 {:title "Table views"
  :idx 52})

(defn render-table [o & [opt]]
  (let [opt (assoc opt :dataspex/view views/table)]
    (hiccup/render-table (data/inspect o opt) opt)))

(defscene map-collection-table
  (render-table
   [{:point/label "Bulbasaur"
     :point/latitude 37.807962
     :point/longitude -122.475238}
    {:point/label "Charmander"
     :point/latitude 34.062759
     :point/longitude -118.35718}
    {:point/label "Squirtle"
     :point/latitude 37.805929
     :point/longitude -122.429582}
    {:point/label "Magnemite"
     :point/latitude 37.8269775
     :point/longitude -122.425144}
    {:point/label "Magmar"
     :point/latitude 37.571414
     :point/longitude -122.00004}]))

(defscene custom-sorted-map-collection-table
  (render-table
   [{:point/label "Bulbasaur"
     :point/latitude 37.807962
     :point/longitude -122.475238}
    {:point/label "Charmander"
     :point/latitude 34.062759
     :point/longitude -118.35718}
    {:point/label "Squirtle"
     :point/latitude 37.805929
     :point/longitude -122.429582}
    {:point/label "Magnemite"
     :point/latitude 37.8269775
     :point/longitude -122.425144}
    {:point/label "Magmar"
     :point/latitude 37.571414
     :point/longitude -122.00004}]
   {:dataspex/path [:dinosaurs]
    :dataspex/sorting
    {[:dinosaurs]
     {:key :point/label
      :order :dataspex.sort.order/descending}}}))
