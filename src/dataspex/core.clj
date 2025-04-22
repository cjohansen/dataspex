(ns dataspex.core
  (:require [dataspex.data :as data]
            [dataspex.inspector :as inspector]
            [dataspex.jwt :as jwt]
            [dataspex.server :as server]))

(data/add-string-inspector! jwt/inspect-jwt)

(defonce store (atom {}))
(defonce server (atom nil))

(defn stop-server! []
  (when-let [current @server]
    (server/stop-server current)
    (reset! server nil)))

(defn start-server!
  {:arglists '[[]
               [{:keys [port]}]]}
  [& [opt]]
  (stop-server!)
  (println "Starting Dataspex server on http://localhost:"
           (or (:port opt) server/default-port))
  (reset! server (server/start-server store opt)))

(defn ^{:export true :indent 1} inspect
  {:arglists '[[label x]
               [label x {:keys [start-server? server-port]}]]}
  [label x & [opt]]
  (inspector/inspect store label x opt)
  (when (and (nil? @server) (not (false? (:start-server? opt))))
    (start-server!))
  nil)
