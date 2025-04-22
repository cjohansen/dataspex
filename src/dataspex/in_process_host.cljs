(ns dataspex.in-process-host
  (:require [dataspex.codec :as codec]
            [dataspex.render-host :as rh]))

(defn post-message [event data]
  (js/window.postMessage
   #js {:from "dataspex-library"
        :payload (codec/generate-string {:event event, :data data})}))

(defn receive-message [^js message request-render process-actions]
  (let [{:keys [event data]} (codec/parse-string (.-payload message))]
    (case event
      :extension-loaded
      (request-render)

      :actions
      (post-message :effects (process-actions data))

      (prn "Unknown Dataspex event" event data))))

(defrecord InProcessHost []
  rh/ClientChannel
  (initialize! [_ request-render process-actions]
    (js/window.addEventListener
     "message"
     (fn [e]
       (when (= "dataspex-extension" (.. e -data -from))
         (receive-message (.-data e) request-render process-actions)))))

  (render [_ hiccup]
    (post-message :render hiccup)))

(defn create-channel []
  (->InProcessHost))
