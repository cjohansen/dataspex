(ns dataspex.browser-extension
  "Main entry-point for the browser extension"
  (:require [dataspex.browser-extension-client :as extension-client]
            [dataspex.render-client :as rc]
            [dataspex.server-client :as server-client]))

(defn ^:export main []
  (let [client (rc/start-render-client js/document.body)
        on-message (fn on-message [{:keys [event data]}]
                     (case event
                       :connect-remote-host
                       (server-client/add-channel client (str (:host data) "/jvm") {:on-message on-message})))]
    (->> (extension-client/create-channel {:on-message on-message})
         (rc/add-channel client "extension"))))
