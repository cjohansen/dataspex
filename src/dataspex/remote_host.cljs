(ns dataspex.remote-host
  (:require [dataspex.codec :as codec]
            [dataspex.render-host :as rh]))

(defn connect-event-source [remote-host host-id process-actions]
  (let [event-source (js/EventSource. (str remote-host "/relay/" host-id "/actions"))
        attempts (atom 3)]
    (.addEventListener event-source "message"
     (fn [e]
       (prn "Remote host actions" (.-data e))
       (process-actions (codec/parse-string (.-data e)))))

    (.addEventListener
     event-source "error"
     (fn [_]
       (if (= 0 @attempts)
         (do
           (.close event-source)
           (println (str "Dataspex couldn't reach the server on " remote-host " after three attempts, giving up. Refresh page to inspect remotely.")))
         (swap! attempts dec))))))

(defrecord RemoteHost [!connected? remote-host host-id]
  rh/ClientChannel
  (initialize! [_ _ process-actions]
    (connect-event-source remote-host host-id process-actions))

  (render [_ hiccup]
    (when @!connected?
      (-> (js/fetch (str remote-host "/relay/" host-id "/renders")
                    #js {:method "POST"
                         :body (codec/generate-string hiccup)})
          (.catch (fn [_] (reset! !connected? false)))))))

(defn get-host-id []
  (hash (str js/navigator.userAgent js/location.origin)))

(defn create-channel [remote-host]
  (->RemoteHost (atom true) remote-host (get-host-id)))
