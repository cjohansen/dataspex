(ns dataspex.views
  (:require [dataspex.data :as data]
            [dataspex.protocols :as dp]))

(def inline ::inline)
(def dictionary ::dictionary)
(def table ::table)
(def source ::source)
(def views #{inline dictionary table source})

(def max-items 100)
(def max-count 1000)

(defn get-pagination [{:dataspex/keys [pagination path]}]
  {:page-size (or (get-in pagination [path :page-size])
                  (:page-size pagination)
                  max-items)
   :offset (or (get-in pagination [path :offset]) 0)})

(defn offset-pagination [opt n]
  [:dataspex.actions/assoc-in
   [(:dataspex/inspectee opt) :dataspex/pagination (:dataspex/path opt) :offset]
   n])

(defn ^{:indent 2} update-folding [opt path v]
  [:dataspex.actions/assoc-in
   [(:dataspex/inspectee opt) :dataspex/folding (into (vec (:dataspex/path opt)) path)] v])

(defn path-to
  ([opt] (path-to opt []))
  ([{:dataspex/keys [path]} xs]
   (into (vec path) xs)))

(defn ^{:indent 1} navigate-to [opt path]
  (cond-> [[:dataspex.actions/assoc-in [(:dataspex/inspectee opt) :dataspex/path] path]]
    (not= :dataspex.activity/browse (:dataspex/activity opt))
    (conj [:dataspex.actions/assoc-in [(:dataspex/inspectee opt) :dataspex/activity] :dataspex.activity/browse])))

(defn ^{:indent 1} get-current-view [v {:dataspex/keys [path view default-view] :as opt}]
  (or (get view path)
      (when (satisfies? dp/IPrefersView v)
        (views (dp/get-preferred-view v)))
      (when (data/supports-view? v default-view opt)
        default-view)
      (when (data/hiccup? v)
        source)
      dictionary))

(defn get-render-data [state inspectee]
  (let [inspector-state (get state inspectee)
        opt (merge {:dataspex/inspectee inspectee}
                   (select-keys inspector-state
                                (->> (keys inspector-state)
                                     (filter (comp #{"dataspex"} namespace)))))
        data (-> (get-in state [inspectee :val])
                 (data/nav-in (:dataspex/path opt)))
        opt (assoc opt :dataspex/view (get-current-view data opt))]
    {:opt opt
     :data (data/inspect data opt)}))
