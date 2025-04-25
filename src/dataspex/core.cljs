(ns dataspex.core
  (:require [dataspex.data :as data]
            [dataspex.in-process-host :as in-process-host]
            [dataspex.inspector :as inspector]
            [dataspex.jwt :as jwt]
            [dataspex.remote-host :as remote-host]
            [dataspex.render-host :as rh]))

(data/add-string-inspector! jwt/inspect-jwt)

(defonce store
  (let [store (atom {})]
    (rh/start-render-host store
      {:channels [(in-process-host/create-channel)
                  (remote-host/create-channel "http://localhost:7117")]})
    store))

(defn ^:export inspect
  {:arglists '[[label x]
               [label x {:keys [track-changes? history-limit max-height]}]]}
  [label x & [opt]]
  (inspector/inspect store label x opt))
