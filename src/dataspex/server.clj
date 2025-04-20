(ns dataspex.server
  (:require [dataspex.actions :as actions]
            [dataspex.panel :as panel]
            [ring.adapter.jetty :as jetty]
            [ring.core.protocols :as protocols]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :as response])
  (:import (java.nio.charset StandardCharsets)))

(defn write-state [store id out state]
  (try
    (.write out (-> (str "data: " (pr-str (panel/render-inspector state)) "\n\n")
                    (.getBytes StandardCharsets/UTF_8)))
    (.flush out)
    (catch java.io.IOException _
      (remove-watch store id))))

(defn events-handler [respond store]
  (respond
   {:status  200
    :headers {"Content-Type" "text/event-stream"
              "Cache-Control" "no-cache"
              "Connection" "keep-alive"}
    :body (let [id (random-uuid)]
            (reify
              protocols/StreamableResponseBody
              (write-body-to-stream [_ _ out]
                (add-watch store id (fn [_ _ _ new-state]
                                      (write-state store id out new-state)))
                (write-state store id out @store))))}))

(defn process-actions [respond req]
  (let [effects (->> (read-string (slurp (:body req)))
                     (actions/plan @(:store req)))]
    (->> (remove (comp #{:effect/copy} first) effects)
         (actions/execute-batched! (:store req)))
    (respond {:status 200
              :body (pr-str (filterv (comp #{:effect/copy} first) effects))})))

(defn app [req respond _raise]
  (cond
    (= "/" (:uri req))
    (respond (response/file-response "resources/public/dataspex/remote-inspector.html"))

    (= "/renders" (:uri req))
    (events-handler respond (:store req))

    (= "/actions" (:uri req))
    (process-actions respond req)

    :else
    (respond {:status 200
              :body "Hello!"})))

(defn ^:export start-server [store & [{:keys [port]}]]
  (-> (fn [req respond raise]
        (app (assoc req :store store) respond raise))
      (wrap-resource "public")
      (jetty/run-jetty
       {:port (or port 7117)
        :async? true
        :join? false})))

(defn ^:export stop-server [^org.eclipse.jetty.server.Server server]
  (.stop server))

(comment

  (def store (atom {}))
  (def server (start-server store))

  (require '[dataspex.inspector :as inspector])

  (inspector/inspect store "Map" {:hello "World!"
                                  :runtime "JVM, baby!"
                                  :numbers (range 1500)})

  (swap! store update :num (fnil inc 0))

  (stop-server server)

)
