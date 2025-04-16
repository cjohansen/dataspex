(ns dataspex.data
  (:require [clojure.datafy :as datafy]
            [dataspex.views :as views]
            [dataspex.protocols :as dp]
            #?(:cljs [dataspex.date :as date])))

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

(defn nav-in
  "Returns the value located at `ks` in a nested structure `x`.

  Works like `clojure.core/get-in`, but supports custom navigation:
  - If `x` (or its inspected form) satisfies `dataspex.protocols/INavigatable`,
    navigation is delegated to it.
  - Otherwise, falls back to standard `get`, `nth`, or `aget` logic depending
    on the structure.

  Returns `x` unchanged if `ks` is empty."
  [x ks]
  (if (empty? ks)
    x
    (if (satisfies? dp/INavigatable x)
      (dp/nav-in x ks)
      (let [data (inspect x)
            k (first ks)]
        (cond
          (satisfies? dp/INavigatable data)
          (dp/nav-in data ks)

          (satisfies? dp/IKeyLookup k)
          (recur (dp/lookup k data) (next ks))

          (or (associative? data) (set? data))
          (recur (get data (first ks)) (next ks))

          :else
          (cond
            (and (coll? data) (number? k))
            (recur (nth data k) (next ks))

            (or (js-array? data) (js-object? data))
            (recur (aget data (cond-> k
                                (keyword? k) name)) (next ks))))))))

(defn as-key [v]
  (if (satisfies? dp/IKey v)
    (dp/to-key v)
    v))

(defn stringify [v]
  (binding [*print-namespace-maps* false]
    (pr-str v)))

(defn type-pref [x]
  (cond
    (qualified-keyword? x) 0
    (keyword? x) 1
    (qualified-symbol? x) 2
    (symbol? x) 3
    (string? x) 4
    (number? x) 5
    (map? x) 6
    (vector? x) 7
    (list? x) 8
    (set? x) 9
    (seq? x) 10
    (boolean? x) 11
    :else 12))

(def sort-order (juxt type-pref str))

(defn get-indexed-entries [coll opt]
  (map-indexed
   (fn [i x]
     {:k i
      :label i
      :v (inspect x opt)})
   coll))

(defn get-set-entries [s opt]
  (->> (cond->> s
         (not (sorted? s))
         (sort-by sort-order))
       (map (fn [v]
              (let [v (inspect v opt)]
                {:k (as-key v)
                 :v v})))))

(defn get-js-array-entries [#?(:cljs ^js arr :clj arr) opt]
  (.map arr (fn [v i]
              {:k i
               :label i
               :v (inspect v opt)})))

#?(:cljs
   (extend-type js/Date
     dp/INavigatable
     (nav-in [d [p & path]]
       (nav-in (get (date/->map d) p) path))))
