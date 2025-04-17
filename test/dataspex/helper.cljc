(ns dataspex.helper
  (:require [datascript.core :as d]
            [dataspex.data :as data]
            [dataspex.hiccup :as hiccup]
            [dataspex.views :as views])
  #?(:cljs (:require-macros dataspex.helper)))

(defmacro with-conn
  {:clj-kondo/lint-as 'clojure.core/let}
  [[binding schema] & body]
  `(let [~binding (datascript.core/create-conn ~schema)]
     ~@body))

(defn render-inline
  ;; Make it thread-last friendly
  ([x] (render-inline nil x))
  ([opt x]
   (let [opt (assoc opt :dataspex/view views/inline)]
     (hiccup/render-inline (data/inspect x opt) opt))))

(defn render-dictionary
  ;; Make it thread-last friendly
  ([v] (render-dictionary nil v))
  ([opt v]
   (let [opt (assoc opt :dataspex/view views/dictionary)]
     (hiccup/render-dictionary (data/inspect v opt) opt))))

(defn render-table
  ;; Make it thread-last friendly
  ([v] (render-table nil v))
  ([opt v]
   (let [opt (assoc opt :dataspex/view views/table)]
     (hiccup/render-table (data/inspect v opt) opt))))

(defn render-source
  ;; Make it thread-last friendly
  ([v] (render-source nil v))
  ([opt v]
   (let [opt (assoc opt :dataspex/view views/source)]
     (hiccup/render-source (data/inspect v opt) opt))))

(defn undatom [x]
  (cond->> x (d/datom? x) (into [])))

(defn undatom-diff [diffs]
  (mapv
   (fn [[path op v]]
     (cond-> [(mapv undatom path) op]
       v (conj (undatom v))))
   diffs))
