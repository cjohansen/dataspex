(ns dataspex.browser-extension
  "Main entry-point for the browser extension"
  (:require [dataspex.browser-extension-client :as extension-client]
            [dataspex.codec :as codec]
            [dataspex.remotes-panel :as remotes]
            [dataspex.render-client :as rc]))

(defn get-inspected-host [f]
  (.eval (.-inspectedWindow js/chrome.devtools)
         "location.host"
         (fn [host _exception?]
           (when host
             (f host)))))

(defn get-persistent-remotes [host f]
  (when (and (.-storage js/chrome) (.-local js/chrome.storage))
    (.get (.-local js/chrome.storage)
          #js [host]
          (fn [result]
            (when-let [v (aget result host)]
              (f (codec/parse-string v)))))))

(defn ^:export main []
  (let [client (rc/start-render-client
                js/document.body
                {:handle-remotes-actions remotes/handle-actions
                 :render-remotes remotes/render-panel})
        on-message (fn on-message [{:keys [event data]}]
                     (case event
                       :connect-remote-host
                       (remotes/add-channel client (str (:host data) "/jvm") {:on-message on-message})

                       :disconnect-remote-host
                       (remotes/remove-channel client (str (:host data) "/jvm"))

                       (println "Unrecognized event" event)))]
    (get-inspected-host
     (fn [inspected-host]
       (swap! client assoc :dataspex/inspected-host inspected-host)
       (get-persistent-remotes
        inspected-host
        (fn [remote-hosts]
          (doseq [remote-host remote-hosts]
            (remotes/add-channel client remote-host {:on-message on-message}))
          (swap! client assoc :dataspex/persistent-remotes remote-hosts)))))
    (->> (extension-client/create-channel {:on-message on-message})
         (rc/add-channel client "extension"))))
