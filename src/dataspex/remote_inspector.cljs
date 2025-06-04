(ns dataspex.remote-inspector
  (:require [dataspex.render-client :as rc]
            [dataspex.server-client :as server-client]))

(defn connect [client host on-message]
  (server-client/add-channel client (str host "/jvm") {:on-message on-message})
  (server-client/add-channel client (str host "/relay")))

(defn disconnect [client host]
  (server-client/remove-channel client (str host "/jvm"))
  (server-client/remove-channel client (str host "/relay")))

(defn ^:export main []
  (let [client (rc/start-render-client js/document.body)
        on-message
        (fn on-message [{:keys [event data]}]
          (case event
            :connect-remote-host (connect client (:host data) on-message)
            :disconnect-remote-host (disconnect client (:host data))))]
    (connect client js/location.origin on-message)))
