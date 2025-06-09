(ns dataspex.render-host
  (:require [clojure.walk :as walk]
            [dataspex.actions :as actions]
            [dataspex.panel :as panel]
            [dataspex.protocols :as dp]
            [dataspex.version :as version]))

(defprotocol ClientChannel
  (initialize! [channel request-render process-actions])
  (render [channel hiccup]))

(defprotocol RemoteManager
  (connect-remote [channel host])
  (disconnect-remote [channel host]))

(def path-cache (atom {}))

(defn strip-opaque-keys [data]
  (walk/prewalk
   (fn [x]
     (if (or (satisfies? dp/IKeyLookup x)
             (record? x))
       (let [id (hash x)]
         (swap! path-cache assoc id x)
         [:dataspex/key id])
       x))
   data))

(defn revive-keys [data]
  (walk/postwalk
   (fn [x]
     (if (and (vector? x) (= :dataspex/key (first x)))
       (get @path-cache (second x))
       x))
   data))

(defn process-actions [store actions]
  (let [effects (->> (revive-keys actions)
                     (actions/plan @store))]
    (->> (remove (comp #{:effect/copy} first) effects)
         (actions/execute-batched! store))
    (filterv (comp #{:effect/copy} first) effects)))

(defn render-inspector [state]
  (strip-opaque-keys (panel/render-inspector state)))

(defn tick [f]
  #?(:cljs (js/requestAnimationFrame f)
     :clj (f)))

(defn get-events [old-state new-state]
  (let [pending (remove (set (:dataspex/remotes old-state)) (:dataspex/remotes new-state))
        expired (remove (set (:dataspex/remotes new-state)) (:dataspex/remotes old-state))]
    (cond-> (if (not= (dissoc old-state :dataspex/remotes)
                      (dissoc new-state :dataspex/remotes))
              [{:event :render
                :data (render-inspector new-state)}]
              [])
      (nil? old-state)
      (conj {:event :connect
             :data {:breaking-version version/breaking-version
                    :version version/version
                    :host-str (:dataspex/host-str new-state)}})

      (seq pending)
      (into (for [host pending]
              {:event :connect-remote-host
               :data {:host host}}))

      (seq expired)
      (into (for [host expired]
              {:event :disconnect-remote-host
               :data {:host host}})))))

(defn ^{:indent 1} start-render-host [store]
  (add-watch
   store ::render
   (fn [_ _ old-state new-state]
     (tick
      #(doseq [event (get-events old-state new-state)]
         (doseq [channel (::channels new-state)]
           (case (:event event)
             :render (render channel (:data event))

             :connect-remote-host
             (when (satisfies? RemoteManager channel)
               (connect-remote channel (:host (:data event))))

             :disconnect-remote-host
             (when (satisfies? RemoteManager channel)
               (disconnect-remote channel (:host (:data event)))))))))))

(defn add-channel [store channel]
  (initialize!
   channel
   #(do
      (render channel (render-inspector @store))
      (when (satisfies? RemoteManager channel)
        (doseq [host (:dataspex/remotes @store)]
          (connect-remote channel host))))
   #(process-actions store %))
  (swap! store update ::channels (fnil conj []) channel)
  store)
