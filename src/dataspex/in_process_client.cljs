(ns dataspex.in-process-client
  "The in-process client runs in the same process that inspects data with
  Dataspex. It's main use is for aiding Dataspex development."
  (:require [dataspex.actions :as actions]
            [dataspex.panel :as panel]
            [dataspex.render-client :as rc]))

(defn create-channel [store]
  (reify
    rc/HostChannel
    (connect [_ render-f]
      (add-watch store ::render (fn [_ _ _ state]
                                  (render-f (panel/render-inspector state)))))

    (disconnect [_]
      (remove-watch store ::render))

    (process-actions [_ _ actions]
      (actions/act! store actions)
      (js/Promise.resolve []))))
