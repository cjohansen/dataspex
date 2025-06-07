(ns dataspex.dev
  (:require [dataspex.core :as dataspex]
            [dataspex.data :as data]
            [dataspex.datascript :as datascript]
            [dataspex.demo-data :as demo-data]
            [dataspex.demo-state :as demo-state]
            [dataspex.in-process-client :as in-process-client]
            [dataspex.inspector :as inspector]
            [dataspex.jwt :as jwt]
            [dataspex.remotes-panel :as remotes]
            [dataspex.render-client :as rc]))

::datascript/keep

(defonce store (atom {}))
(data/add-string-inspector! jwt/inspect-jwt)

(defonce txes
  (demo-data/add-data demo-data/conn))

(def client (rc/start-render-client js/document.body
                                    {:handle-remotes-actions remotes/handle-actions
                                     :render-remotes remotes/render-panel}))
(rc/add-channel client "process" (in-process-client/create-channel store))

(defn ^:dev/after-load main []
  (swap! store assoc ::loaded (.getTime (js/Date.))))

(dataspex/inspect "App state" demo-state/app-state {:dataspex/ns-aliases {'really.long.ns 'rl}})
(dataspex/inspect-remote "http://localhost:7117")
(dataspex/connect-remote-inspector "http://localhost:7117")

(inspector/inspect store "App state" demo-state/app-state {:dataspex/ns-aliases {'really.long.ns 'rl}})

(inspector/inspect store "Test" {:val (keyword ":ns" "keyword")})

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
