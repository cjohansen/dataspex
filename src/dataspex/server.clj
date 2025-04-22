(ns dataspex.server
  (:require [dataspex.codec :as codec]
            [dataspex.inspector :as inspector]
            [dataspex.render-host :as rh]
            [ring.adapter.jetty :as jetty]
            [ring.core.protocols :as protocols]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :as response])
  (:import (java.nio.charset StandardCharsets)))

(defn write-state [store id out state]
  (try
    (.write out (-> (str "data: " (codec/generate-string (rh/render-inspector state)) "\n\n")
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

(defn process-actions [req]
  {:status 200
   :body (->> (slurp (:body req))
              codec/parse-string
              (rh/process-actions (:store req))
              codec/generate-string)})

(defn app [req respond _raise]
  (cond
    (= "/jvm/renders" (:uri req))
    (events-handler respond (:store req))

    :else
    (respond
     (cond
       (= "/" (:uri req))
       (response/file-response "resources/public/dataspex/remote-inspector.html")

       (= "/jvm/actions" (:uri req))
       (process-actions req)))))

(defn wrap-cors [handler]
  (fn [req respond raise]
    (let [respond-with-cors
          (fn [res]
            (-> res
                (assoc-in [:headers "Access-Control-Allow-Origin"] "*")
                (assoc-in [:headers "Access-Control-Allow-Methods"] "GET, PUT, POST, DELETE, HEAD, OPTIONS")
                respond))]
      (if (= :options (:request-method req))
        (respond-with-cors {:status 200})
        (handler req respond-with-cors raise)))))

(defn ^:export start-server [store & [{:keys [port]}]]
  (-> (fn [req respond raise]
        (app (assoc req :store store) respond raise))
      (wrap-resource "public")
      (wrap-cors)
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

  (inspector/inspect store "Map" {:greeting {:hello "World"}
                                  :runtime "JVM, baby!"
                                  :numbers (range 1500)})

  (inspector/inspect store "Bob" dataspex.datomic/bob)

  (reset! store {})

  (swap! store update :num (fnil inc 0))

  (stop-server server)

)
