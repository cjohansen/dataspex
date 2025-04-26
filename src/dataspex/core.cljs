(ns dataspex.core
  (:require [clojure.string :as str]
            [dataspex.data :as data]
            [dataspex.in-process-host :as in-process-host]
            [dataspex.inspector :as inspector]
            [dataspex.jwt :as jwt]
            [dataspex.remote-host :as remote-host]
            [dataspex.render-host :as rh]
            [dataspex.user-agent :as ua]))

(data/add-string-inspector! jwt/inspect-jwt)

(defn- get-host-str [{:keys [browser os]} origin]
  (str (str/replace origin #"^https?://" "") " " browser " " os))

(defonce store
  (let [store (atom {:dataspex/host-str (get-host-str (ua/parse-user-agent) js/location.origin)})]
    (rh/start-render-host store
      {:channels [(in-process-host/create-channel)
                  (remote-host/create-channel "http://localhost:7117")]})
    store))

(defn ^:export inspect
  {:arglists '[[label x]
               [label x {:keys [track-changes? history-limit max-height]}]]}
  [label x & [opt]]
  (inspector/inspect store label x opt))

(defn ^:export uninspect [label]
  (inspector/uninspect store label))
