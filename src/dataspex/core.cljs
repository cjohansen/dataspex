(ns dataspex.core
  "The Dataspex public API for ClojureScript."
  (:require [clojure.string :as str]
            [dataspex.codec :as codec]
            [dataspex.data :as data]
            [dataspex.in-process-host :as in-process-host]
            [dataspex.inspector :as inspector]
            [dataspex.jwt :as jwt]
            [dataspex.remote-host :as remote-host]
            [dataspex.render-host :as render-host]
            [dataspex.tap-inspector :as tap-inspector]
            [dataspex.user-agent :as ua]
            [goog.functions :as gfn]
            dataspex.datascript
            dataspex.error))

:dataspex.datascript/keep
:dataspex.error/keep
(data/add-string-inspector! jwt/inspect-jwt)

(defn- get-host-str [{:keys [browser os]} origin]
  (str (str/replace origin #"^https?://" "") " " browser " " os))

(def persist!
  (gfn/debounce
   (fn [state]
     (try
       (->> (select-keys state (filter string? (keys state)))
            (mapv (fn [[k v]]
                    [k (select-keys v (filter (comp #{"dataspex"} namespace) (keys v)))]))
            (into {})
            codec/generate-string
            (.setItem js/localStorage "dataspex"))
       (catch :default _)))
   500))

(defonce store
  (if (exists? js/location)
    (let [host-str (get-host-str (ua/parse-user-agent) js/location.origin)
          store (atom (-> (some-> (try
                                    (js/localStorage.getItem "dataspex")
                                    (catch :default _ nil))
                                  codec/parse-string)
                          (assoc :dataspex/host-str host-str)))]
      (add-watch store ::remember (fn [_ _ _ state] (persist! state)))
      (render-host/start-render-host store)
      (render-host/add-channel store (in-process-host/create-channel host-str))
      store)
    (atom nil)))

(defn ^:export connect-remote-inspector
  "Connect a server to send inspected data to for remote viewing. Sending to a
  remote allows data to be inspected without using the Dataspex browser
  extension (e.g. Safari, mobile browsers). `host` defaults to
  \"http://localhost:7117\"."
  [& [host]]
  (render-host/add-channel store (remote-host/create-channel (or host "http://localhost:7117"))))

(defn ^:export inspect
  {:arglists '[[label x]
               [label x {:keys [track-changes? history-limit max-height ns-aliases]}]]}
  [label x & [opt]]
  (inspector/inspect store label x opt)
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

(defn ^:export inspect-remote [host]
  (swap! store update :dataspex/remotes (fnil conj #{}) host)
  nil)

(defn ^:export uninspect-remote [host]
  (swap! store update :dataspex/remotes disj host)
  nil)
