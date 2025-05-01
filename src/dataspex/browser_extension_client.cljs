(ns dataspex.browser-extension-client
  (:require [dataspex.codec :as codec]
            [dataspex.render-client :as rc]))

(def firefox?
  (re-find #"Firefox" (.-userAgent js/navigator)))

(defn post-message [message]
  (js/chrome.devtools.inspectedWindow.eval
   (str "window.postMessage({ from: \"dataspex-extension\", payload: '"
        (codec/generate-string message)
        "' })")))

(defn post-actions [actions]
  (js/Promise.resolve (post-message {:event :actions :data actions})))

(defn process-message [render ^js message]
  (when (= "dataspex-library" (.-from message))
    (let [message (codec/parse-string (.-payload message))]
      (case (:event message)
        :render (render (:data message))
        :effects (rc/execute-effects (:data message))
        (js/console.log "Unknown message event" (codec/generate-string message))))))

(defn subscribe-to-messages [render]
  (when (and js/chrome js/chrome.runtime)
    (if firefox?
      (-> (js/chrome.runtime.connect #js {:name "dataspex-panel"})
          .-onMessage
          (.addListener
           (fn [^js message]
             (process-message render message))))
      (js/chrome.runtime.onMessage.addListener
       (fn [^js message _sender _send-response]
         (process-message render message))))))

(defrecord BrowserExtensionClient []
  rc/HostChannel
  (initialize! [_ render-f]
    (js/chrome.runtime.sendMessage #js {:type "panel-ready"})
    (subscribe-to-messages render-f)
    (post-message {:event :extension-loaded}))

  (process-actions [_ _ actions]
    (post-actions actions)))

(defn create-channel []
  (->BrowserExtensionClient))
