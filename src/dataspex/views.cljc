(ns dataspex.views
  (:require [dataspex.data :as data]))

(def inline ::inline)
(def dictionary ::dictionary)
(def table ::table)
(def source ::source)

(defn get-pagination [{:dataspex/keys [pagination path]}]
  {:page-size (or (get-in pagination [path :page-size])
                  (:page-size pagination)
                  1000)
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
  [:dataspex.actions/assoc-in [(:dataspex/inspectee opt) :dataspex/path] path])

(defn get-current-view [v {:dataspex/keys [path view default-view] :as opt}]
  (or (get view path)
      (when (data/supports-view? v default-view opt)
        default-view)
      dictionary))

(defn get-view-options [state inspectee]
  (-> (get state inspectee)
      (assoc :dataspex/inspectee inspectee)))
