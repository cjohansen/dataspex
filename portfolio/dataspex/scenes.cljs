(ns dataspex.scenes
  (:require [dataspex.audit-scenes]
            [dataspex.dictionary-scenes]
            [dataspex.inline-scenes]
            [dataspex.source-scenes]
            [dataspex.table-scenes]
            [dataspex.test.inline-scenes]
            [dataspex.ui-scenes]
            [portfolio.data :as data]
            [portfolio.ui :as portfolio]
            [replicant.dom :as r]))

:dataspex.audit-scenes/keep
:dataspex.dictionary-scenes/keep
:dataspex.inline-scenes/keep
:dataspex.source-scenes/keep
:dataspex.table-scenes/keep
:dataspex.test.inline-scenes/keep
:dataspex.ui-scenes/keep

(data/register-collection!
 :dataspex
 {:title "UI elements"
  :idx 0})

(data/register-collection!
 :dataspex.test
 {:title "Integration tests"
  :idx 1})

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
