(ns dataspex.views)

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

(defn path-to
  ([opt] (path-to opt []))
  ([{:dataspex/keys [path]} xs]
   (into (vec path) xs)))

(defn navigate-to [opt path]
  [:dataspex.actions/assoc-in [(:dataspex/inspectee opt) :dataspex/path] path])
