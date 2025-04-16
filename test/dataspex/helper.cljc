(ns dataspex.helper
  (:require [dataspex.data :as data]
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
