(ns dataspex.codec
  #?(:cljs (:require [cljs.reader :as reader])))

(defn parse-string [data-string]
  #?(:clj (read-string data-string)
     :cljs (reader/read-string data-string)))

(defn generate-string [data]
  (pr-str data))
