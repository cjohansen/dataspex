(ns dataspex.codec
  (:require [cognitect.transit :as transit])
  #?(:clj (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
                   [java.nio.charset StandardCharsets])))

(def fmt :json)

(defn parse-string [s]
  (when (not-empty s)
    #?(:cljs (transit/read (transit/reader fmt) s)
       :clj (-> (.getBytes s StandardCharsets/UTF_8)
                ByteArrayInputStream.
                (transit/reader fmt)
                transit/read))))

(defn generate-string [data]
  #?(:cljs (transit/write (transit/writer fmt) data)
     :clj (let [out (ByteArrayOutputStream. 4096)
                writer (transit/writer out fmt)]
            (transit/write writer data)
            (.toString out))))
