(ns dataspex.helper
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [datascript.core :as d]
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
  (cond
    (or (d/datom? x) (and (not (set? x)) (:e x)))
    (let [[e a v t add?] x]
      [e a v t add?])

    (coll? x)
    (mapv undatom x)

    :else x))

(defn undatom-diff [diffs]
  (mapv
   (fn [[path op v]]
     (cond-> [(mapv undatom path) op]
       v (conj (undatom v))))
   diffs))

(defn strip-attrs [hiccup & [attrs]]
  (let [[hiccup attrs] (if (set? hiccup)
                         [attrs hiccup]
                         [hiccup attrs])
        f (if attrs
            #(empty? (apply dissoc % attrs))
            (constantly true))]
    (walk/postwalk
     (fn [x]
       (if (and (data/hiccup? x)
                (map? (second x))
                (f (second x)))
         (into [(first x)] (drop 2 x))
         x))
     hiccup)))

(defn strip-clock-times [data]
  (walk/postwalk
   (fn [x]
     (cond-> x
       (string? x)
       (str/replace #"\d\d:\d\d:\d\d" "HH:mm:ss")))
   data))
