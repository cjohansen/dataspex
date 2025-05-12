(ns dataspex.inspector
  (:require [dataspex.data :as data]
            [dataspex.diff :as diff]
            [dataspex.protocols :as dp])
  #?(:clj (:import (java.util Date))))

(defn get-dataspex-opts [current {:keys [host-str label auditable? max-height]}]
  (cond-> {:dataspex/path []
           :dataspex/activity :dataspex.activity/browse}
    host-str (assoc :dataspex/host-str host-str)
    label (assoc :dataspex/inspectee label)
    (not= nil auditable?) (assoc :dataspex/auditable? auditable?)
    (number? max-height) (assoc :dataspex/max-height max-height)
    :then
    (into (->> (keys current)
               (filter (comp #{"dataspex"} namespace))
               (select-keys current)))))

(defn ^:no-doc inspect-val [current x opt & [diff]]
  (if (= x (:val current))
    current
    (let [rev (inc (or (:rev current) 0))]
      (merge
       current
       (get-dataspex-opts current opt)
       (cond-> {:rev rev
                :val x}
         (:subscription opt) (assoc :subscription (:subscription opt))
         (:ref opt) (assoc :ref (:ref opt))

         (:track-changes? opt)
         (assoc :history
                (let [summary (dp/get-audit-summary x)
                      details (dp/get-audit-details x)
                      diff (or diff (some-> (:history current) first :val (diff/diff x)))]
                  (->> (cond-> {:created-at (:now opt)
                                :rev rev
                                :val x}
                         diff (assoc :diff diff)
                         summary (assoc :dataspex.audit/summary summary)
                         details (assoc :dataspex.audit/details details))
                       (conj (:history current))
                       (take (:history-limit opt))))))))))

(defn- now []
  #?(:cljs (js/Date.)
     :clj (Date.)))

(defn ^:no-doc get-opts [opt]
  (cond-> (assoc opt :now (now))
    (not (contains? opt :track-changes?))
    (assoc :track-changes? true)

    (not (number? (:history-limit opt)))
    (assoc :history-limit 25)))

(extend-type #?(:cljs cljs.core/Atom
                :clj clojure.lang.IAtom)
  dp/Watchable
  (get-val [ref]
    @ref)

  (watch [ref k f]
    (add-watch ref k (fn [_ _ old-data new-data] (f old-data new-data nil)))
    k)

  (unwatch [ref k]
    (remove-watch ref k)))

(defn try-extend-inspectee [x]
  (cond
    (satisfies? dp/Watchable x)
    x

    (data/watchable? x)
    (reify
      dp/Watchable
      (get-val [_]
        @x)

      (watch [_ k f]
        (add-watch x k (fn [_ _ old-data new-data] (f old-data new-data nil)))
        k)

      (unwatch [_ k]
        (remove-watch x k)))

    :else x))

(defn inspect
  {:arglists '[[store label x]
               [store label x {:keys [track-changes? history-limit max-height]}]]}
  [store label x & [opt]]
  (let [x (try-extend-inspectee x)
        [val subscription]
        (if (satisfies? dp/Watchable x)
          [(dp/get-val x)
           (dp/watch x ::inspect
                     (fn [_ new-val diff]
                       (swap! store update label inspect-val new-val (get-opts opt) diff)))]
          [x])]
    (->> (cond-> (assoc (get-opts opt)
                        :label label
                        :host-str (:dataspex/host-str @store))
           subscription (assoc :subscription subscription
                               :ref x))
         (swap! store update label inspect-val val))))

(defn uninspect [store label]
  (let [{:keys [ref subscription]} (get @store label)]
    (when subscription
      (dp/unwatch ref subscription)))
  (swap! store dissoc label))
