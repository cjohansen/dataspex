(ns dataspex.scenes
  (:require [dataspex.dictionary-scenes]
            [dataspex.inline-scenes]
            [dataspex.source-scenes]
            [dataspex.table-scenes]
            [portfolio.data :as data]
            [portfolio.ui :as portfolio]
            [replicant.dom :as r]))

:dataspex.dictionary-scenes/keep
:dataspex.inline-scenes/keep
:dataspex.source-scenes/keep
:dataspex.table-scenes/keep

(data/register-collection!
 :dataspex
 {:title "UI elements"
  :idx 0})

(r/set-dispatch!
 (fn [_ event-data]
   (prn event-data)))

(def light-theme
  {:background/background-color "#fff"
   :background/document-class "light"})

(def dark-theme
  {:background/background-color "#18181a"
   :background/document-class "dark"})

(defn ^:export main []
  (portfolio/start!
   {:config
    {:css-paths ["/dataspex/inspector.css"]
     :background/options
     [{:id :default
       :title "Light"
       :value light-theme}
      {:id :replicant
       :title "Dark"
       :value dark-theme}]

     :canvas/layout {:kind :rows
                     :xs [light-theme
                          dark-theme]}}}))
