(ns dataspex.inspector
  (:require [dataspex.diff :as diff]
            [dataspex.protocols :as dp])
  #?(:clj (:import (java.util Date))))

(defn ^:no-doc inspect-val [current x {:keys [track-changes? history-limit
                                              now ref label auditable?]}]
  (let [prev (first (:history current))
        rev (inc (or (:rev current) 0))]
    (merge
     {:dataspex/path []
      :dataspex/activity :dataspex.activity/browse}
     (when (not= nil auditable?)
       {:dataspex/auditable? auditable?})
     (when label
       {:dataspex/inspectee label})
     (->> (keys current)
          (filter (comp #{"dataspex"} namespace))
          (select-keys current))
     (cond-> {:rev rev
              :val x}
       ref (assoc :ref ref)

       track-changes?
       (assoc :history
              (let [summary (dp/get-audit-summary x)
                    details (dp/get-audit-details x)]
                (->> (cond-> {:created-at now
                              :rev rev
                              :val x}
                       prev (assoc :diff (diff/diff (:val prev) x))
                       summary (assoc :dataspex.audit/summary summary)
                       details (assoc :dataspex.audit/details details))
                     (conj (:history current))
                     (take history-limit))))))))

(defn- now []
  #?(:cljs (js/Date.)
     :clj (Date.)))

(defn ^:no-doc get-opts [opt]
  (cond-> (assoc opt :now (now))
    (not (contains? opt :track-changes?))
    (assoc :track-changes? true)

    (not (number? (:history-limit opt)))
    (assoc :history-limit 25)))

(defn- ref? [x]
  (when x
    #?(:clj (instance? clojure.lang.IDeref x)
       :cljs (satisfies? cljs.core/IDeref x))))

(defn inspect [store label x & [opt]]
  (let [[val ref] (if (ref? x) [@x x] [x])]
    (swap! store update label inspect-val val (assoc (get-opts opt)
                                                     :ref ref
                                                     :label label))
    (when ref
      (add-watch ref ::inspect
       (fn [_ _ _ new-val]
         (swap! store update label inspect-val new-val (get-opts opt)))))))

(defn uninspect [store label]
  (when-let [ref (get-in @store [label :ref])]
    (remove-watch ref ::inspect))
  (swap! store dissoc label))
