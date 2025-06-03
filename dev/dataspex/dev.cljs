(ns dataspex.dev
  (:require [dataspex.core :as dataspex]
            [dataspex.data :as data]
            [dataspex.datascript :as datascript]
            [dataspex.demo-data :as demo-data]
            [dataspex.demo-state :as demo-state]
            [dataspex.in-process-client :as in-process-client]
            [dataspex.inspector :as inspector]
            [dataspex.jwt :as jwt]
            [dataspex.panel :as panel]
            [dataspex.render-client :as rc]))

::datascript/keep

(defonce store (atom {}))
(data/add-string-inspector! jwt/inspect-jwt)

(defonce txes
  (demo-data/add-data demo-data/conn))

(rc/start-render-client js/document.body
 {:channels {:process (in-process-client/create-channel store)}})

(defn ^:dev/after-load main []
  (swap! store assoc ::loaded (.getTime (js/Date.))))

;;(dataspex/inspect "App state" demo-state/app-state)

(inspector/inspect store "App state" demo-state/app-state)

(comment
  (dataspex/inspect "DB" demo-data/conn)

  (inspector/inspect store "DB" demo-data/conn)
  (inspector/inspect store "Hiccup" (panel/render-panel @store "App state"))

  (js/setTimeout
   #(dataspex/inspect  "App state" (assoc-in demo-state/app-state [:filters :search-term] "batman"))
   500)

  (js/setTimeout
   #(dataspex/inspect  "App state" (-> demo-state/app-state
                                       (update-in [:filters :genres] (fn [gs] (vec (next gs))))
                                       (assoc-in [:filters :search-term] "batman")))
   2500))
