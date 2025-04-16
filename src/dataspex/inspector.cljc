(ns dataspex.inspector
  (:require [dataspex.diff :as diff])
  #?(:clj (:import (java.util Date))))

(defn ^:no-doc inspect-val [current x {:keys [track-changes? history-limit now]}]
  (let [prev (first (:history current))
        rev (inc (or (:rev current) 0))]
    (merge
     {:dataspex/path []
      :dataspex/activity :dataspex.activity/browse}
     (->> (keys current)
          (filter (comp #{"dataspex"} namespace))
          (select-keys current))
     (cond-> {:rev rev
              :val x}
       track-changes?
       (assoc :history
              (->> (cond-> {:created-at now
                            :rev rev
                            :val x}
                     prev (assoc :diff (diff/diff (:val prev) x)))
                   (conj (:history current))
                   (take history-limit)))))))

(defn- now []
  #?(:cljs (js/Date.)
     :clj (Date.)))

(defn ^:no-doc get-opts [opt]
  (cond-> (assoc opt :now (now))
    (not (contains? opt :track-changes?))
    (assoc :track-changes? true)

    (not (number? (:history-limit opt)))
    (assoc :history-limit 25)))

(defn inspect [store label x & [opt]]
  (swap! store update label inspect-val x (get-opts opt)))
