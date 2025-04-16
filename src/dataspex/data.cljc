(ns dataspex.data
  (:require [clojure.datafy :as datafy]
            [dataspex.views :as views]
            [dataspex.protocols :as dp]))

(defn js-array? [#?(:cljs v :clj _)]
  #?(:cljs (array? v)))

(defn js-object? [#?(:cljs v :clj _)]
  #?(:cljs (and (not (array? v)) (object? v))))

(def string-inspectors (atom []))

(defn ^:export add-string-inspector!
  "Add a function `f` that can convert a string to inspectable data. The function
  will be called with a string, and should either return a value to inspect or
  `nil` if it has no specific meaning. Custom string inspectors can be used to
  make hashed or otherwise encoded strings inspectable by decoding them into
  structured data. Examples include JWTs (see `dataspex.jwt`), query parameters,
  string-encoded JSON/EDN, encrypted data, stack traces strings, etc."
  [f]
  (swap! string-inspectors conj f))

(defn inspect
  "Converts `x` into inspectable data.

  This is a wrapper around `clojure.datafy/datafy` that prefers values
  implementing the relevant Dataspex rendering protocol for the given `view`. If
  `x` is a string, registered string inspectors (see `add-string-inspector!`)
  will be tried first.

  Returns a value suitable for visualization in the Dataspex UI."
  ([x] (inspect x nil))
  ([x {:dataspex/keys [view]}]
   (let [data (if (string? x)
                (some #(% x) (conj @string-inspectors identity))
                x)]
     (cond
       (and (= view views/inline)
            (satisfies? dp/IRenderInline data))
       data

       (and (= view views/dictionary)
            (satisfies? dp/IRenderDictionary data))
       data

       (and (= view views/table)
            (satisfies? dp/IRenderTable data))
       data

       (and (= view views/source)
            (satisfies? dp/IRenderSource data))
       data

       (or (js-object? data) (js-array? data))
       data

       :else
       (datafy/datafy data)))))
