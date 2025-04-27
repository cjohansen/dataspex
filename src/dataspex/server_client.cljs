(ns dataspex.server-client
  (:require [dataspex.codec :as codec]
            [dataspex.render-client :as rc]))

(defn connect-event-source [host render]
  (let [event-source (js/EventSource. (str host "/renders"))]
    (.addEventListener
     event-source "message"
     (fn [e]
       (println "Render")
       (render (codec/parse-string (.-data e)))))

    (.addEventListener
     event-source "error"
     (fn [e]
       (js/console.error "EventSource error:" e)))))

(defn post-actions [host node actions]
  (let [host-id (some-> node
                        (.closest "[data-host]")
                        (.getAttribute "data-host"))]
    (-> (js/fetch (str host (when host-id (str "/" host-id)) "/actions")
                  (clj->js {:method "POST"
                            :headers {"Content-Type" "application/edn"}
                            :body (codec/generate-string actions)}))
        (.then (fn [res] (.text res)))
        (.then (fn [text] (codec/parse-string text))))))

(defrecord ServerClient [host]
  rc/HostChannel
  (initialize! [_ render-f]
    (connect-event-source host render-f))

  (process-actions [_ node actions]
    (post-actions host node actions)))

(defn create-channel [& [host]]
  (ServerClient. host))
