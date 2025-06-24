(ns dataspex.test.dictionary-scenes
  (:require [datascript.core :as d]
            [dataspex.data :as data]
            [dataspex.datascript]
            [dataspex.hiccup :as hiccup]
            [dataspex.test.data :as test-data]
            [dataspex.views :as views]
            [portfolio.replicant :as portfolio :refer-macros [defscene]]))

:dataspex.datascript/keep

(portfolio/configure-scenes
 {:title "Dictionary views"
  :idx 51})

(defn render-dictionary [o]
  (let [opt {:dataspex/view views/dictionary}]
    (hiccup/render-dictionary (data/inspect o opt) opt)))

(defscene vector-dictionary
  (render-dictionary
   (with-meta
     (->> ["Replicant" "Portfolio" "Dataspex" "m1p" "lookup" "phosphor-clj"]
          (mapv (fn [lib] {:library/name lib})))
     {:secret "Additional data"})))

(defscene set-dictionary
  (render-dictionary
   #{"Replicant" "Portfolio" "Dataspex" "m1p" "lookup" "phosphor-clj"}))

(defscene set-meta-dictionary
  (render-dictionary
   (with-meta
     #{"Replicant" "Portfolio" "Dataspex" "m1p" "lookup" "phosphor-clj"}
     {:secret "Additional data"})))

(defscene js-date-dictionary
  :title "JS Date dictionary"
  (render-dictionary (js/Date. 2025 3 11 17 42 13)))

(defscene js-object-dictionary
  :title "JS Object dictionary"
  (render-dictionary
   #js {:name "Christian" :library "Dataspex"}))

(defscene js-array-dictionary
  :title "JS Array dictionary"
  (render-dictionary
   #js ["Christian" "Dataspex"]))

(defscene datascript-conn-dictionary
  (render-dictionary test-data/conn))

(defscene datascript-db-dictionary
  (render-dictionary (d/db test-data/conn)))

(defscene datom-dictionary
  (render-dictionary (first (:eavt (d/db test-data/conn)))))

(defscene datascript-index-as-dictionary
  (render-dictionary (:eavt (d/db test-data/conn))))

(defscene entity-as-dictionary
  (render-dictionary (d/entity (d/db test-data/conn) 2)))

(defscene event
  (let [event (js/MouseEvent. "click" #js {:bubbles true
                                           :cancelable true
                                           :view js/window})]
    (js/document.body.dispatchEvent event)
    (render-dictionary event)))

(defscene element
  (let [el (js/document.createElement "a")]
    (.setAttribute el "data-custom-number" 42)
    (set! (.-href el) "https://replicant.fun")
    (set! (.-id el) "link")
    (set! (-> el .-style .-color) "red")
    (-> el .-classList (.add "btn"))
    (-> el .-classList (.add "btn-primary"))
    (set! (.-innerHTML el) "Replicant")
    (render-dictionary el)))

(defscene error
  (render-dictionary
   (ex-info "That didn't go too well!"
            {:data 42}
            (ex-info "Because of this shit!" {:data :boo}))))
