(ns dataspex.server-client
  "The server client connects to a Dataspex ring server. It receives hiccup to
  render over a server-sent events endpoint and sends commands with an HTTP POST
  request."
  (:require [dataspex.codec :as codec]
            [dataspex.render-client :as rc]
            [dataspex.version :as version]))

(defn connect-event-source [!state host render on-message on-connection-status-changed]
  (let [event-source (js/EventSource. (str host "/client-messages"))]
    (swap! !state merge {:event-source event-source
                         :attempts 3})
    (on-connection-status-changed {:connected? true})
    (.addEventListener
     event-source "message"
     (fn [e]
       (let [{:keys [event data]} (codec/parse-string (.-data e))]
         (println event)
         (case event
           :connect (when-let [hiccup (version/check-version data)]
                      (render hiccup {:error? true}))
           :render (render data)
           (if on-message
             (on-message {:event event :data data})
             (println "Unknown event" event data))))))

    (.addEventListener
     event-source "error"
     (fn [_]
       (if (= 0 (:attempts @!state))
         (do
           (.close event-source)
           (println "Dataspex couldn't reach the server on " host " after three attempts, giving up. Refresh page to inspect remotely.")
           (swap! !state dissoc :event-source)
           (on-connection-status-changed {:connected? true}))
         (swap! !state update :attempts dec))))))

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

(defn ^{:indent 1} create-channel [& [host {:keys [on-connection-status-changed on-message]}]]
  (let [!state (atom {})]
    (reify
      rc/HostChannel
      (connect [_ render-f]
        (connect-event-source !state host render-f on-message (or on-connection-status-changed (fn [_]))))

      (disconnect [_]
        (.close (:event-source @!state))
        (on-connection-status-changed {:connected? false})
        (reset! !state {}))

      (process-actions [_ node actions]
        (post-actions host node actions)))))
