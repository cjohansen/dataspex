(ns dataspex.remote-inspector
  (:require [dataspex.render-client :as rc]
            [dataspex.server-client :as server-client]))

(defn ^:export main []
  (rc/start-render-client js/document.body
   {:channels
    {:jvm (server-client/create-channel "/jvm")
     :remotes (server-client/create-channel "/relay")}}))
