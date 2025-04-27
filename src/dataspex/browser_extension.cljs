(ns dataspex.browser-extension
  (:require [dataspex.browser-extension-client :as extension-client]
            [dataspex.render-client :as rc]
            [dataspex.server-client :as server-client]))

(defn ^:export main []
  (rc/start-render-client
   {:channels
    {:server (server-client/create-channel "http://localhost:7117/jvm")
     :extension (extension-client/create-channel)}}))
