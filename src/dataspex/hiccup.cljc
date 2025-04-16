(ns dataspex.hiccup
  (:require [dataspex.data :as data]
            [dataspex.protocols :as dp]
            [dataspex.ui :as-alias ui]
            [dataspex.views :as views]))

(defn inflect [n w]
  ;; It ain't much, but it works for the symbolic types Dataspex knows about
  (str n " " w (when-not (= 1 n) "s")))

(declare bounded-size)

(defn bounded-coll-size [n coll]
  (loop [size 1
         xs coll]
    (cond
      (<= n size) n
      (empty? xs) size
      :else (recur (+ size 1 (bounded-size (- n size) (first xs))) (next xs)))))

(defn bounded-size [n v]
  (min n
       (cond
         (= n 0) 0

         (keyword? v)
         (+ (or (some-> v namespace count inc) 0)
            (-> v name count inc))

         (string? v)
         (+ 2 (count v))

         (true? v)
         4

         (false? v)
         5

         (map? v)
         (loop [size 0
                xs v]
           (cond
             (<= n size) n
             (empty? xs) size
             :else (let [[k v] (first xs)
                         size (+ size 3) ;; space between key and value,
                         ;; plus comma and space before next value, OR
                         ;; parenthesis (the last element)
                         k-size (bounded-size (- n size) k)]
                     (recur (+ size k-size (bounded-size (- n size k-size) v)) (next xs)))))

         (set? v)
         (inc (bounded-coll-size n v))

         (coll? v)
         (bounded-coll-size n v)

         (number? v)
         (count (str v))

         :else
         (count (pr-str v)))))

(defn type-name [x]
  (cond
    (keyword? x) "keyword"
    (symbol? x) "symbol"
    (string? x) "string"
    (number? x) "number"
    (map? x) "map"
    (vector? x) "vector"
    (list? x) "list"
    (set? x) "set"
    (seq? x) "seq"
    (boolean? x) "boolean"
    (nil? x) "nil"
    :else
    (if-let [[_ _ tn] (re-find #"(?i)(.*/)?([a-z]+)$" (pr-str (type x)))]
      tn
      "object")))

(defn summarize? [v {:dataspex/keys [summarize-above-w]}]
  (let [w (or summarize-above-w 120)]
    (when (< 0 w)
      (< (dec w) (bounded-size w v)))))

(defn summarize [xs & [{:keys [kind]}]]
  (if (< 1000 (bounded-count 1001 xs))
    "1000+ items"
    (let [types (set (mapv type-name xs))]
      (inflect
       (count xs)
       (cond
         kind kind
         (< 1 (count types)) "item"
         :else (first types))))))

(def tag->brackets
  {::ui/list ["(" ")"]
   ::ui/vector ["[" "]"]
   ::ui/set ["#{" "}"]})

(defn paginate [{:keys [offset page-size]} xs]
  (cond->> xs
    offset (drop offset)
    page-size (take page-size)))

(declare render-inline)

(defn ^{:indent 1} render-paginated-sequential [tag s opt & [get-entries]]
  (if (summarize? s opt)
    (let [[l r] (tag->brackets tag)]
      [::ui/link (str l (summarize s) r)])
    (let [{:keys [page-size offset]} (views/get-pagination opt)
          current-end (+ offset page-size)
          more (- (bounded-count (+ current-end 1001) s) current-end)
          attrs (select-keys opt [::ui/prefix])]
      (into
       (cond-> [tag]
         (not-empty attrs) (conj attrs))
       (let [opt (dissoc opt ::ui/prefix)]
         (cond-> []
           (< 0 offset)
           (conj [::ui/link
                  {::ui/actions
                   (views/offset-pagination opt (max 0 (- offset page-size)))}
                  (str offset " more")])

           :then (into
                  (->> ((or get-entries data/get-indexed-entries) s opt)
                       (paginate {:page-size page-size, :offset offset})
                       (mapv #(render-inline (:v %) (update opt ::path conj (:k %))))))

           (< current-end (bounded-count (inc (+ offset page-size)) s))
           (conj [::ui/link
                  {::ui/actions
                   (views/offset-pagination opt (+ offset page-size))}
                  (str (if (< 1000 more) "1000+" more) " more")])))))))

(defn render-inline-seq [s opt]
  (render-paginated-sequential ::ui/list s opt data/get-indexed-entries))

(defn render-inline-set [s opt]
  (render-paginated-sequential ::ui/set s opt data/get-set-entries))

(defn render-inline-array [a opt]
  (render-paginated-sequential ::ui/vector a (assoc opt ::ui/prefix "#js") data/get-js-array-entries))

(defn render-inline-map [m entries opt]
  (let [prefix (::ui/prefix opt)
        opt (dissoc opt ::ui/prefix)]
    (if (summarize? m opt)
      (let [ks (map :k entries)]
        (if (summarize? ks opt)
          [::ui/link
           (str prefix (when prefix " ")
                "{" (summarize ks {:kind "key"}) "}")]
          (into (cond-> [::ui/map]
                  prefix (conj {::ui/prefix prefix}))
                (mapv (fn [k]
                        [::ui/map-entry
                         (render-inline k opt)])
                      ks))))
      (into (cond-> [::ui/map]
              prefix (conj {::ui/prefix prefix}))
            (mapv (fn [{:keys [label v]}]
                    [::ui/map-entry
                     (render-inline label opt)
                     (render-inline v opt)])
                  entries)))))

(defn render-inline-object [o opt]
  (cond
    (map? o) (render-inline-map o (data/get-map-entries o opt) opt)
    (coll? o) (render-inline-seq o opt)
    (data/js-array? o) (render-inline-array o opt)
    (data/js-object? o) (render-inline-map o (data/get-js-object-entries o opt) (assoc opt ::ui/prefix "#js"))

    :else
    (let [string (data/stringify o)]
      (if-let [[_ prefix s] (re-find #"(#[a-zA-Z_\-*+!?=<>][a-zA-Z0-9_\-*+!?=<>/.]+)\s(.+)$" string)]
        [::ui/literal {::ui/prefix prefix}
         (if-let [[_ s] (re-find #"^\"(.*)\"$" s)]
           [::ui/string s]
           [::ui/code s])]
        [::ui/code string]))))

(defn render-inline [data & [opt]]
  (if (satisfies? dp/IRenderInline data)
    (dp/render-inline data opt)
    (render-inline-object data opt)))

(extend-type #?(:cljs string
                :clj java.lang.String)
  dp/IRenderInline
  (render-inline [s _]
    [::ui/string s]))

(extend-type #?(:cljs cljs.core/Keyword
                :clj clojure.lang.Keyword)
  dp/IRenderInline
  (render-inline [k _]
    [::ui/keyword k]))

(extend-type #?(:cljs number
                :clj java.lang.Number)
  dp/IRenderInline
  (render-inline [n _]
    [::ui/number n]))

(extend-type #?(:cljs boolean
                :clj java.lang.Boolean)
  dp/IRenderInline
  (render-inline [b _]
    [::ui/boolean b]))

(extend-type #?(:cljs cljs.core/Symbol
                :clj clojure.lang.Symbol)
  dp/IRenderInline
  (render-inline [s _]
    [::ui/symbol s]))

(extend-type #?(:cljs cljs.core/PersistentVector
                :clj clojure.lang.PersistentVector)
  dp/IRenderInline
  (render-inline [v opt]
    (render-paginated-sequential ::ui/vector v opt data/get-indexed-entries)))

(extend-type #?(:cljs cljs.core/List
                :clj clojure.lang.PersistentList)
  dp/IRenderInline
  (render-inline [l opt]
    (render-paginated-sequential ::ui/list l opt)))

#?(:clj
   (extend-type clojure.lang.ISeq
     dp/IRenderInline
     (render-inline [s opt]
       (render-inline-seq s opt))))

(extend-type #?(:cljs cljs.core/PersistentHashSet
                :clj clojure.lang.PersistentHashSet)
  dp/IRenderInline
  (render-inline [s opt]
    (render-inline-set s opt)))

(extend-type #?(:cljs cljs.core/Atom
                :clj clojure.lang.IAtom)
  dp/IRenderInline
  (render-inline [r opt]
    (render-paginated-sequential ::ui/vector [(deref r)] (assoc opt ::ui/prefix "#atom"))))
