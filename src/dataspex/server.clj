(ns dataspex.server
  (:require [clojure.string :as str]
            [dataspex.codec :as codec]
            [dataspex.render-host :as render-host]
            [ring.adapter.jetty :as jetty]
            [ring.core.protocols :as protocols]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :as response])
  (:import (java.nio.charset StandardCharsets)))

(defn write-event [ref id ^java.io.OutputStream out data]
  (try
    (let [bytes (-> (str "data: " (codec/generate-string data) "\n\n")
                    (.getBytes StandardCharsets/UTF_8))]
      (.write out bytes 0 (alength bytes)))
    (.flush out)
    (catch java.io.IOException _
      (remove-watch ref id))
    (catch Exception e
      (prn e))))

(defn ^{:indent 2} stream-changes [ref ^java.io.OutputStream out f {:keys [stream-current?]}]
  (let [id (random-uuid)]
    (add-watch ref id (fn [_ _ old-state new-state]
                        (doseq [event (f old-state new-state)]
                          (write-event ref id out event))))
    (if-not (false? stream-current?)
      (doseq [event (f nil @ref)]
        (write-event ref id out event))
      (.flush out))))

(defn render-relayed-renders [_ state]
  [{:event :render
    :data
    (some->> (sort-by key state)
             (keep
              (fn [[id hiccup]]
                (when hiccup
                  [:article {:data-host id}
                   hiccup])))
             not-empty
             (into [:div]))}])

(defn get-relayed-actions [host-id data]
  (when (= (:host-id data) host-id)
    (:actions data)))

(defn events-handler [respond ref f & [{:keys [stream-current?]}]]
  (respond
   {:status  200
    :headers {"Content-Type" "text/event-stream"
              "Cache-Control" "no-cache"
              "Connection" "keep-alive"}
    :body (reify
            protocols/StreamableResponseBody
            (write-body-to-stream [_ _ out]
              (stream-changes ref out f {:stream-current? stream-current?})))}))

(defn get-body [req]
  (codec/parse-string (slurp (:body req))))

(defn process-actions [req]
  {:status 200
   :body (->> (get-body req)
              (render-host/process-actions (:store req))
              codec/generate-string)})

(defn relay-request [req respond]
  (let [[_ _ host-id path] (str/split (:uri req) #"/")]
    (if (= :post (:request-method req))
      (let [data (get-body req)]
        (cond
          (= "renders" path)
          (swap! (:relay-renders req) assoc host-id data)

          (= "actions" path)
          (reset! (:relay-actions req) {:host-id host-id, :actions data}))
        (respond {:status 200}))
      (cond
        (= "/relay/client-messages" (:uri req))
        (events-handler respond (:relay-renders req) render-relayed-renders)

        (= "actions" path)
        (events-handler respond (:relay-actions req) #(vector (get-relayed-actions host-id %2)) {:stream-current? false})))))

(defn app [req respond _raise]
  (cond
    (= "/jvm/client-messages" (:uri req))
    (events-handler respond (:store req) render-host/get-events)

    (str/starts-with? (:uri req) "/relay/")
    (relay-request req respond)

    :else
    (respond
     (cond
       (= "/" (:uri req))
       (response/resource-response "public/dataspex/remote-inspector.html")

       (= "/jvm/actions" (:uri req))
       (process-actions req)))))

(defn wrap-cors [handler]
  (fn [req respond raise]
    (let [respond-with-cors
          (fn [res]
            (-> res
                (assoc-in [:headers "Access-Control-Allow-Origin"] "*")
                (assoc-in [:headers "Access-Control-Allow-Methods"] "GET, PUT, POST, DELETE, HEAD, OPTIONS")
                (assoc-in [:headers "Access-Control-Allow-Headers"] "content-type")
                respond))]
      (if (= :options (:request-method req))
        (respond-with-cors {:status 200})
        (handler req respond-with-cors raise)))))

(def default-port 7117)

(defn ^:export start-server [store & [{:keys [port]}]]
  (let [relay-renders (atom {})
        relay-actions (atom {})]
    (-> (fn [req respond raise]
          (-> (assoc req :store store)
              (assoc :relay-renders relay-renders)
              (assoc :relay-actions relay-actions)
              (app respond raise)))
        (wrap-resource "public")
        (wrap-cors)
        (jetty/run-jetty
         {:port (or port default-port)
          :async? true
          :join? false}))))

(defn ^:export stop-server [^org.eclipse.jetty.server.Server server]
  (.stop server))
