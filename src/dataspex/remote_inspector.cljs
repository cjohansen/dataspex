(ns dataspex.remote-inspector
  (:require [dataspex.render-client :as rc]
            [dataspex.server-channel :as server-channel]))

(defn ^:export main []
  (rc/start-render-client
   {:channels
    {:jvm (server-channel/create-channel "/jvm")}}))
