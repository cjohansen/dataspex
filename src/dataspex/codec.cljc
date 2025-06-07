(ns dataspex.codec
  (:require [cognitect.transit :as transit])
  #?(:clj (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
                   [java.nio.charset StandardCharsets])))

(def fmt :json)

#?(:cljs (def reader (transit/reader fmt)))
#?(:cljs (def writer (transit/writer fmt)))

(defn parse-string [s]
  (when (not-empty s)
    #?(:cljs (transit/read reader s)
       :clj (-> (.getBytes s StandardCharsets/UTF_8)
                ByteArrayInputStream.
                (transit/reader fmt)
                transit/read))))

(defn generate-string [data]
  #?(:cljs (transit/write writer data)
     :clj (let [out (ByteArrayOutputStream. 4096)
                writer (transit/writer out fmt)]
            (transit/write writer data)
            (.toString out))))
