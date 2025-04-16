(ns dataspex.test.inline-scenes
  (:require [datascript.core :as d]
            [dataspex.data :as data]
            [dataspex.datascript]
            [dataspex.hiccup :as hiccup]
            [dataspex.test.data :as test-data]
            [dataspex.views :as views]
            [portfolio.replicant :as portfolio :refer-macros [defscene]]))

:dataspex.datascript/keep

(portfolio/configure-scenes
 {:title "Inline views"
  :idx 50})

(defn render-inline [o]
  (let [opt {:dataspex/view views/inline}]
    (hiccup/render-inline (data/inspect o opt) opt)))

(defscene inline-atom
  (render-inline (atom {:state "Here"})))

(defscene inline-js-array
  :title "Inline JS Array"
  (render-inline #js ["a" "b" "c"]))

(defscene inline-js-object
  :title "Inline JS Object"
  (render-inline #js {:name "Christian", :keys 2}))

(defscene inline-js-date
  :title "Inline JS Date"
  (render-inline (js/Date.)))

(defscene inline-datascript-conn
  (render-inline test-data/conn))

(defscene inline-datascript-db
  (render-inline (d/db test-data/conn)))

(defscene inline-datom
  (render-inline (first (:eavt (d/db test-data/conn)))))

(defscene inline-tiny-datascript-index
  (render-inline (:eavt (d/db test-data/tiny-conn))))

(defscene inline-datascript-index
  (render-inline (:eavt (d/db test-data/conn))))

(defscene inline-entity
  (render-inline (d/entity (d/db test-data/conn) 1)))
