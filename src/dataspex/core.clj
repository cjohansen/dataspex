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
  (reset! server (server/start-server store opt))
  (println (str "Started Dataspex server on http://localhost:" (:port @server))))

(defn ^{:export true :indent 1} inspect
  {:arglists '[[label x]
               [label x {:keys [start-server? server-port ns-aliases]}]]}
  [label x & [opt]]
  (when (nil? (:dataspex/host-str @store))
    (swap! store assoc :dataspex/host-str (get-host-str)))
  (inspector/inspect store label x opt)
  (when-let [port (:server-port opt)]
    (let [running @server]
      (when (and running (not= port (:port running)) (not= port 0))
        (println
         (str "A Dataspex server is already running on port " (:port running)
              ", will not start another on port " port)))))
  (when (and (nil? @server) (not (false? (:start-server? opt))))
    (start-server! {:port (:server-port opt)}))
  x)

(defn ^:export uninspect [label]
  (inspector/uninspect store label)
  nil)

(defn ^:export inspect-taps
  {:arglists '[[]
               [label]
               [label {:keys [start-server? server-port]}]]}
  [& [label opt]]
  (inspect (or label "Taps") (tap-inspector/create-inspector) (assoc opt :track-changes? false)))

(defn ^:export uninspect-taps [& [label]]
  (uninspect (or label "Taps")))

(defn ^:export inspect-remote [host]
  (swap! store update :dataspex/remotes (fnil conj #{}) host)
  nil)

(defn ^:export uninspect-remote [host]
  (swap! store update :dataspex/remotes disj host)
  nil)

(comment

  (stop-server!)
  (start-server!)
  (get-host-str)

)
