(ns dataspex.core
  "The Dataspex public API for JVM Clojure"
  (:require [dataspex.data :as data]
            [dataspex.inspector :as inspector]
            [dataspex.jwt :as jwt]
            [dataspex.server :as server]
            [dataspex.tap-inspector :as tap-inspector]
            dataspex.datascript))

:dataspex.datascript/keep

(try
  (require 'dataspex.datomic)
  (catch Throwable _ false))

(data/add-string-inspector! jwt/inspect-jwt)

(defn- get-host-str []
  (str (-> (System/getProperty "user.dir")
           java.io.File.
           .getName)
       (if-let [port (try
                       (slurp ".nrepl-port")
                       (catch Exception _
                         nil))]
         (str " nrepl:" port)
         " JVM")))

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
  (when (nil? (:dataspex/host-str @store))
    (swap! store assoc :dataspex/host-str (get-host-str)))
  (stop-server!)
  (println "Starting Dataspex server on http://localhost:"
           (or (:port opt) server/default-port))
  (reset! server (server/start-server store opt)))

(defn ^{:export true :indent 1} inspect
  {:arglists '[[label x]
               [label x {:keys [start-server? server-port]}]]}
  [label x & [opt]]
  (when (nil? (:dataspex/host-str @store))
    (swap! store assoc :dataspex/host-str (get-host-str)))
  (inspector/inspect store label x opt)
  (when (and (nil? @server) (not (false? (:start-server? opt))))
    (start-server!))
  x)

(defn ^:export uninspect [label]
  (inspector/uninspect store label)
  nil)

(defn ^:export inspect-taps [& [label]]
  (inspector/inspect
   store
   (or label "Taps")
   (tap-inspector/create-inspector)
   {:track-changes? false})
  nil)

(defn ^:export uninspect-taps [& [label]]
  (uninspect (or label "Taps")))

(comment

  (stop-server!)
  (start-server!)
  (get-host-str)

)
