(ns dataspex.remote-inspector
  (:require [dataspex.remotes-panel :as remotes]
            [dataspex.render-client :as rc]))

(defn connect [client host on-message]
  (remotes/add-channel client (str host "/jvm") {:on-message on-message})
  (remotes/add-channel client (str host "/relay")))

(defn disconnect [client host]
  (remotes/remove-channel client (str host "/jvm"))
  (remotes/remove-channel client (str host "/relay")))

(defn ^:export main []
  (let [client (rc/start-render-client js/document.body
                                       {:handle-remotes-actions remotes/handle-actions
                                        :render-remotes remotes/render-panel})
        on-message
        (fn on-message [{:keys [event data]}]
          (case event
            :connect-remote-host (connect client (:host data) on-message)
            :disconnect-remote-host (disconnect client (:host data))))]
    (connect client js/location.origin on-message)))
