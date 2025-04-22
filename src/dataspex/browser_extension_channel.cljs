(ns dataspex.browser-extension-channel
  (:require [dataspex.codec :as codec]
            [dataspex.render-client :as rc]))

(defn post-message [message]
  (js/chrome.devtools.inspectedWindow.eval
   (str "window.postMessage({ from: \"dataspex-extension\", payload: '"
        (codec/generate-string message)
        "' })")))

(defn post-actions [actions]
  (js/Promise.resolve (post-message {:event :actions :data actions})))

(defn subscribe-to-messages [render]
  (when (and js/chrome js/chrome.runtime)
    (js/chrome.runtime.onMessage.addListener
     (fn [^js message _sender _send-response]
       (when (= "dataspex-library" (.-from message))
         (let [message (codec/parse-string (.-payload message))]
           (case (:event message)
             :render (render (:data message))
             :effects (rc/execute-effects (:data message))
             (js/console.log "Unknown message event" (codec/generate-string message)))))))))

(defrecord BrowserExtensionChannel []
  rc/HostChannel
  (initialize! [_ render-f]
    (subscribe-to-messages render-f)
    (post-message {:event :extension-loaded}))

  (process-actions [_ actions]
    (post-actions actions)))

(defn create-channel []
  (->BrowserExtensionChannel))
