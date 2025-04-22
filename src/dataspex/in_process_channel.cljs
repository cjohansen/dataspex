(ns dataspex.in-process-channel
  (:require [dataspex.actions :as actions]
            [dataspex.panel :as panel]
            [dataspex.render-client :as rc]))

(defrecord InProcessChannel [store]
  rc/HostChannel
  (initialize! [_ render-f]
    (add-watch store ::render (fn [_ _ _ state]
                                (render-f (panel/render-inspector state)))))

  (process-actions [_ actions]
    (actions/act! store actions)
    (js/Promise.resolve [])))

(defn create-channel [store]
  (->InProcessChannel store))
