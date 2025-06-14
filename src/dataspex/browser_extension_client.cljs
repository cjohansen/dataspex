(ns dataspex.browser-extension-client
  "The browser extension client expects to run in the browser devtools extension.
  It connects to the inspected page via a browser-specific messaging channel."
  (:require [dataspex.codec :as codec]
            [dataspex.render-client :as rc]
            [dataspex.version :as version]))

(def firefox?
  (when (exists? js/navigator)
    (re-find #"Firefox" (.-userAgent js/navigator))))

(defn post-message [message]
  (js/chrome.devtools.inspectedWindow.eval
   (str "window.postMessage({ from: \"dataspex-extension\", payload: '"
        (codec/generate-string message)
        "' })")))

(defn post-actions [actions]
  (js/Promise.resolve (post-message {:event :actions :data actions})))

(defn process-message [^js message render on-message]
  (when (= "dataspex-library" (.-from message))
    (let [message (codec/parse-string (.-payload message))]
      (case (:event message)
        :connect (when-let [hiccup (version/check-version (:data message))]
                   (render hiccup {:error? true}))
        :render (render (:data message))
        :effects (rc/execute-effects (:data message))
        (if on-message
          (on-message message)
          (js/console.log "Unknown message event" (codec/generate-string message)))))))

(defn subscribe-to-messages [render on-message]
  (when (and js/chrome js/chrome.runtime)
    (if firefox?
      (-> (js/chrome.runtime.connect #js {:name "dataspex-panel"})
          .-onMessage
          (.addListener
           (fn [^js message]
             (process-message message render on-message))))
      (js/chrome.runtime.onMessage.addListener
       (fn [^js message _sender _send-response]
         (process-message message render on-message))))))

(defn create-channel [{:keys [on-message]}]
  (reify
    rc/HostChannel
    (connect [_ render-f]
      (js/chrome.runtime.sendMessage #js {:type "panel-ready"})
      (subscribe-to-messages render-f on-message)
      (post-message {:event :extension-loaded}))

    (disconnect [_])

    (process-actions [_ _ actions]
      (post-actions actions))))
