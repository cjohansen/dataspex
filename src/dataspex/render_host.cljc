(ns dataspex.render-host
  (:require [clojure.walk :as walk]
            [dataspex.actions :as actions]
            [dataspex.panel :as panel]
            [dataspex.protocols :as dp]))

(defprotocol ClientChannel
  (initialize! [channel request-render process-actions])
  (render [channel hiccup]))

(def path-cache (atom {}))

(defn strip-opaque-keys [data]
  (walk/postwalk
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

(defn ^{:indent 1} start-render-host [store {:keys [channels]}]
  (doseq [channel channels]
    (initialize!
     channel
     #(render channel (render-inspector @store))
     #(process-actions store %)))
  (add-watch
   store ::render
   (fn [_ _ _ new-state]
     (tick
      #(let [hiccup (render-inspector new-state)]
         (doseq [channel channels]
           (render channel hiccup)))))))
