(ns dataspex.browser-extension
  (:require [dataspex.browser-extension-channel :as extension-channel]
            [dataspex.render-client :as rc]
            [dataspex.server-channel :as server-channel]))

(defn ^:export main []
  (rc/start-render-client
   {:channels
    {:server (server-channel/create-channel "http://localhost:7117/jvm")
     :extension (extension-channel/create-channel)}}))
