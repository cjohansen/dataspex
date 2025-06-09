(ns dataspex.in-process-host
  (:require [dataspex.codec :as codec]
            [dataspex.render-host :as rh]
            [dataspex.version :as version]))

(defn post-message [event data]
  (js/window.postMessage
   #js {:from "dataspex-library"
        :payload (codec/generate-string {:event event, :data data})}))

(defn receive-message [host-str ^js message request-render process-actions]
  (let [{:keys [event data]} (codec/parse-string (.-payload message))]
    (case event
      :extension-loaded
      (do
        (post-message :connect {:breaking-version version/breaking-version
                                :version version/version
                                :host-str host-str})
        (request-render))

      :actions
      (post-message :effects (process-actions data))

      (prn "Unknown Dataspex event" event data))))

(defrecord InProcessHost [host-str]
  rh/ClientChannel
  (initialize! [_ request-render process-actions]
    (js/window.addEventListener
     "message"
     (fn [e]
       (when (= "dataspex-extension" (.. e -data -from))
         (receive-message host-str (.-data e) request-render process-actions)))))

  (render [_ hiccup]
    (post-message :render hiccup))

  rh/RemoteManager
  (connect-remote [_ host]
    (post-message :connect-remote-host {:host host}))

  (disconnect-remote [_ host]
    (post-message :disconnect-remote-host {:host host})))

(defn create-channel [host-str]
  (->InProcessHost host-str))
