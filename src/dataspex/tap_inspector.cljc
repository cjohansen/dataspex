(ns dataspex.tap-inspector
  (:require [dataspex.data :as data]
            [dataspex.hiccup :as hiccup]
            [dataspex.protocols :as dp]
            [dataspex.time :as time]
            [dataspex.ui :as-alias ui]))

(defrecord TapKey [idx tapped-at]
  dp/IRenderInline
  (render-inline [_ _]
    [::ui/code (time/hh:mm:ss tapped-at)]))

(defn get-taps [taps]
  (->> taps
       (map-indexed
        (fn [idx {:keys [tapped-at value]}]
          (let [k (->TapKey idx tapped-at)]
            {:label k
             :k k
             :v value})))))

(defn nav-in-taps [taps [k & ks]]
  (let [idx (if (instance? TapKey k) (:idx k) k)]
    (-> (nth taps idx)
        :value
        (data/nav-in ks))))

(deftype TapInspector [store inspector-opts]
  dp/INavigatable
  (nav-in [_ path]
    (nav-in-taps @store path))

  dp/IRenderDictionary
  (render-dictionary [_ opt]
    (hiccup/render-entries-dictionary @store opt (get-taps @store)))

  dp/Watchable
  (get-val [self]
    self)

  (watch [self k f]
    (add-watch store k (fn [_ _ _ _]
                         (f nil self nil)))
    (let [tapper (fn [x]
                   (swap! store #(take (:history-limit inspector-opts)
                                       (conj % {:tapped-at (time/now)
                                                :value x}))))]
      (add-tap tapper)
      {:k k, :tapper tapper}))

  (unwatch [_ subscription]
    (remove-watch store (:k subscription))
    (remove-tap (:tapper subscription))))

(defn create-inspector [& [{:keys [history-limit]}]]
  (->TapInspector (atom (list)) {:history-limit (or history-limit 100)}))
