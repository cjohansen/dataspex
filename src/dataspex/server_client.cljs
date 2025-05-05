(ns dataspex.server-client
  "The server client connects to a Dataspex ring server. It receives hiccup to
  render over a server-sent events endpoint and sends commands with an HTTP POST
  request."
  (:require [dataspex.codec :as codec]
            [dataspex.render-client :as rc]))

(defn connect-event-source [host render]
  (let [event-source (js/EventSource. (str host "/renders"))
        attempts (atom 3)]
    (.addEventListener
     event-source "message"
     (fn [e]
       (println "Render")
       (render (codec/parse-string (.-data e)))))

    (.addEventListener
     event-source "error"
     (fn [_]
       (if (= 0 @attempts)
         (do
           (.close event-source)
           (println "Dataspex couldn't reach the server on localhot:7117 after three attempts, giving up. Refresh page to inspect remotely."))
         (swap! attempts dec))))

    event-source))

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

(defn create-channel [& [host]]
  (let [!event-source (atom nil)]
    (reify
      rc/HostChannel
      (connect [_ render-f]
        (reset! !event-source (connect-event-source host render-f)))

      (disconnect [_]
        (.close @!event-source))

      (process-actions [_ node actions]
        (post-actions host node actions)))))
