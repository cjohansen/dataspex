(ns dataspex.in-process-host
  (:require [dataspex.codec :as codec]
            [dataspex.render-host :as rh]
            [dataspex.version :as version]))

(defn post-message [!state event data]
  (if (:connected? @!state)
    (js/window.postMessage
     #js {:from "dataspex-library"
          :payload (codec/generate-string {:event event, :data data})})
    (swap! !state update :queue (fnil conj []) [event data])))

(defn receive-message [!state host-str ^js message request-render process-actions]
  (let [{:keys [event data]} (codec/parse-string (.-payload message))]
    (case event
      :extension-loaded
      (do
        (post-message !state :connect {:breaking-version version/breaking-version
                                       :version version/version
                                       :host-str host-str})
        (request-render))

      :actions
      (post-message !state :effects (process-actions data))

      (prn "Unknown Dataspex event" event data))))

(defrecord InProcessHost [!state host-str]
  rh/ClientChannel
  (initialize! [_ request-render process-actions]
    (js/window.addEventListener
     "message"
     (fn [e]
       (if (= "dataspex-extension" (.. e -data -from))
         (receive-message !state host-str (.-data e) request-render process-actions)
         (when (= "dataspex-content-script" (.. e -data -from))
           (swap! !state assoc :connected? true)
           (doseq [[event data] (:queue @!state)]
             (post-message !state event data))
           (swap! !state dissoc :queue))))))

  (render [_ hiccup]
    (post-message !state :render hiccup))

  rh/RemoteManager
  (connect-remote [_ host]
    (post-message !state :connect-remote-host {:host host}))

  (disconnect-remote [_ host]
    (post-message !state :disconnect-remote-host {:host host})))

(defn create-channel [host-str]
  (->InProcessHost (atom {:connected? false}) host-str))
