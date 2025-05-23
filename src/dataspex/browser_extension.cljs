(ns dataspex.browser-extension
  "Main entry-point for the browser extension"
  (:require [dataspex.browser-extension-client :as extension-client]
            [dataspex.render-client :as rc]
            [dataspex.server-client :as server-client]))

(defn ^:export main []
  (rc/start-render-client js/document.body
   {:channels
    {:server (server-client/create-channel "http://localhost:7117/jvm")
     :extension (extension-client/create-channel)}}))
