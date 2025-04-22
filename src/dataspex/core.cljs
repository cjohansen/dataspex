(ns dataspex.core
  (:require [dataspex.data :as data]
            [dataspex.in-process-host :as iph]
            [dataspex.inspector :as inspector]
            [dataspex.jwt :as jwt]
            [dataspex.render-host :as rh]))

(data/add-string-inspector! jwt/inspect-jwt)

(defonce store (atom {}))

(rh/start-render-host store
  {:channels [(iph/create-channel)]})

(defn ^:export inspect
  {:arglists '[[label x]
               [label x {:keys [track-changes? history-limit max-height]}]]}
  [label x & [opt]]
  (inspector/inspect store label x opt))
