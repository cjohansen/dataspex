(ns dataspex.hiccup
  (:require [clojure.string :as str]
            [dataspex.actions :as-alias actions]
            [dataspex.data :as data]
            [dataspex.icons :as-alias icons]
            [dataspex.protocols :as dp]
            [dataspex.ui :as-alias ui]
            [dataspex.views :as views]
            #?(:cljs [dataspex.date :as date])
            #?(:cljs [dataspex.element :as element])))

(defn inflect [n w]
  ;; It ain't much, but it works for the symbolic types Dataspex knows about
  (if (and (< 1 n) (str/ends-with? w "y") (not (str/ends-with? w "ey")))
    (str (apply str (butlast w)) "ies")
    (str w (when-not (= 1 n) "s"))))

(defn enumerate [n w]
  (str n " " (inflect n w)))

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

         (data/js-object? v)
         (loop [size 0
                ks (into [] #?(:cljs (js/Object.keys v)))]
           (cond
             (<= n size) n
             (empty? ks) size
             :else (let [k (first ks)
                         v (aget v k)
                         size (+ size 3) ;; space between key and value,
                         ;; plus comma and space before next value, OR
                         ;; parenthesis (the last element)
                         k-size (bounded-size (- n size) k)]
                     (recur (+ size k-size (bounded-size (- n size k-size) v)) (next ks)))))


         :else
         (count (pr-str v)))))

(defrecord StringLabel [s]
  dp/IRenderInline
  (render-inline [_ _]
    [::ui/code s]))

(defn string-label [s]
  (StringLabel. s))

(defn add-attr [hiccup k v]
  (if (map? (second hiccup))
    (assoc-in hiccup [1 k] v)
    (into [(first hiccup) {k v}] (rest hiccup))))

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
  (let [w (or summarize-above-w 80)]
    (when (< 0 w)
      (< (dec w) (bounded-size w v)))))

(defn summarize [xs & [{:keys [kind]}]]
  (if (< views/max-items (bounded-count (inc views/max-items) xs))
    (str views/max-items "+ items")
    (let [types (set (mapv type-name xs))]
      (enumerate
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

(defn ^{:indent 2} with-pagination-meta [xs pagination hiccup]
  (with-meta
    hiccup
    (let [n (bounded-count (+ (:offset pagination) (inc views/max-count)) xs)]
      (when (< (:page-size pagination) n)
        {:dataspex/pagination (assoc pagination :n n)}))))

(declare render-inline)

(defn get-js-prefix [o]
  (str "#js" (when-let [n (data/get-js-constructor o)] (str "/" n))))

(defn ^{:indent 1} render-paginated-sequential [tag s opt & {:keys [get-entries
                                                                    element-width]}]
  (if (summarize? s opt)
    (let [[l r] (tag->brackets tag)]
      [::ui/link (str l (summarize s) r)])
    (let [{:keys [page-size offset] :as pagination} (views/get-pagination opt)
          current-end (+ offset page-size)
          more (- (bounded-count (+ current-end (inc views/max-items)) s) current-end)
          attrs (select-keys opt [::ui/prefix])
          element-width (or element-width
                            (when (< 0 (or (:dataspex/summarize-above-w opt) 120))
                              20))
          entries ((or get-entries data/get-indexed-entries) s opt)]
      (with-pagination-meta entries pagination
        (into
         (cond-> [tag]
           (not-empty attrs) (conj attrs))
         (let [opt (cond-> (dissoc opt ::ui/prefix)
                     element-width (assoc :dataspex/summarize-above-w element-width))]
           (cond-> []
             (< 0 offset)
             (conj [::ui/link
                    {::ui/actions
                     (views/offset-pagination opt (max 0 (- offset page-size)))}
                    (str offset " more")])

             :then (into
                    (->> (paginate {:page-size page-size, :offset offset} entries)
                         (mapv #(render-inline (:v %) (update opt :dataspex/path conj (:k %))))))

             (< current-end (bounded-count (inc (+ offset page-size)) s))
             (conj [::ui/link
                    {::ui/actions
                     (views/offset-pagination opt (+ offset page-size))}
                    (str (if (< views/max-items more) (str views/max-items "+") more) " more")]))))))))

(defn render-inline-seq [s opt]
  (->> {:get-entries data/get-indexed-entries}
       (render-paginated-sequential ::ui/list s opt)))

(defn render-inline-set [s opt]
  (->> {:get-entries data/get-set-entries}
       (render-paginated-sequential ::ui/set s opt)))

(defn render-inline-array [a opt]
  (->> {:get-entries data/get-js-array-entries}
       (render-paginated-sequential ::ui/vector a (assoc opt ::ui/prefix "#js"))))

(defn render-inline-js-coll [coll opt]
  (->> {:get-entries data/get-indexed-entries}
       (render-paginated-sequential ::ui/vector coll (assoc opt ::ui/prefix (get-js-prefix coll)))))

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

(defn render-inline-js-map [m opt]
  (let [opt (assoc opt ::ui/prefix (get-js-prefix m))]
    (render-inline-map m (data/get-map-entries (data/js-map->map m) opt) opt)))

(defn render-inline-atom [r opt]
  (render-paginated-sequential ::ui/vector [(deref r)] (assoc opt ::ui/prefix "#atom")))

(defn render-inline-object [o opt]
  (cond
    (map? o) (render-inline-map o (data/get-map-entries o opt) opt)
    (coll? o) (render-inline-seq o opt)
    (uuid? o) [::ui/literal {::ui/prefix "#uuid"} [::ui/string (str o)]]
    (data/js-collection? o) (render-inline-js-coll o opt)
    (data/js-map? o) (render-inline-js-map o opt)
    (data/js-array? o) (render-inline-array o opt)
    (data/js-object? o) (render-inline-map o (data/get-inline-js-object-entries o opt) (assoc opt ::ui/prefix (get-js-prefix o)))
    (data/derefable? o) (render-inline-atom o opt)

    :else
    (let [string (data/stringify o)]
      (if-let [[_ prefix s] (re-find #"(#[a-zA-Z_\-*+!?=<>][a-zA-Z0-9_\-*+!?=<>/.]+)\s(.+)$" string)]
        [::ui/literal {::ui/prefix prefix}
         (if-let [[_ s] (re-find #"^\"(.*)\"$" s)]
           [::ui/string s]
           [::ui/code s])]
        [::ui/code string]))))

(defn render-copy-button [opt & paths]
  [::ui/button
   {::ui/title "Copy to clipboard"
    ::ui/actions [[::actions/copy (:dataspex/inspectee opt) (views/path-to opt paths)]]}
   [::icons/copy]])

(defn render-primitive-dictionary [type-name v opt]
  [::ui/dictionary
   [::ui/entry
    [::ui/symbol "Type"]
    [::ui/symbol type-name]]
   [::ui/entry
    [::ui/symbol "Value"]
    (render-inline v {})
    (render-copy-button opt)]])

(defn render-entries-dictionary [v opt entries]
  (let [pagination (views/get-pagination opt)
        rows (into (data/get-meta-entries v)
                   (-> (views/get-pagination opt)
                       (paginate entries)))]
    (with-pagination-meta entries pagination
      (cond-> [::ui/dictionary]
        (every? nil? (mapv :label rows))
        (conj {:class :keyless})

        :then
        (into
         (for [{:keys [k path label v copyable?]} rows]
           (let [opt (cond-> opt
                       k (update :dataspex/path conj k)
                       path (update :dataspex/path into path))]
             [::ui/entry
              {::ui/actions
               (when (or k path)
                 (views/navigate-to opt (views/path-to opt)))}
              (or (some-> label (render-inline opt)) "")
              (render-inline v opt)
              ;; Explicitly compare to false to default to true
              (when-not (false? copyable?)
                (render-copy-button opt))])))))))

(defn ^{:indent 2} update-sorting [opt k v]
  [::actions/assoc-in [(:dataspex/inspectee opt) :dataspex/sorting (:dataspex/path opt) k] v])

(defn render-table-header [k label sort-k sort-order opt]
  [::ui/th
   {::ui/actions
    (cond-> []
      (not= k sort-k)
      (conj (update-sorting opt :key k))

      :then
      (conj (update-sorting opt :order
              (if (or (not= k sort-k)
                      (and (= k sort-k)
                           (= sort-order :dataspex.sort.order/descending)))
                :dataspex.sort.order/ascending
                :dataspex.sort.order/descending))))}
   (some-> label (render-inline opt))
   (when (= sort-k k)
     (if (= :dataspex.sort.order/descending sort-order)
       [:dataspex.icons/sort-descending]
       [:dataspex.icons/sort-ascending]))])

(defn render-map-table
  ([xs opt]
   ;; Assume there are no new keys beyond the first 1000 items. Tables work best
   ;; for homogenous collections, and we can't realize an infinite seq, so that
   ;; seems like a reasonable compromise.
   (render-map-table xs (->> (take 1000 xs)
                             (mapcat keys)
                             set
                             (sort-by data/sort-order)) opt))
  ([xs ks opt]
   (let [sort-k (or (get-in opt [:dataspex/sorting (:dataspex/path opt) :key]) :dataspex/idx)
         sort-order (or (get-in opt [:dataspex/sorting (:dataspex/path opt) :order]) :dataspex.sort.order/asc)
         pagination (views/get-pagination opt)
         opt (update opt :dataspex/summarize-above-w #(or % (/ 190 (count ks))))]
     (with-pagination-meta xs pagination
       [::ui/table
        (-> [::ui/thead
             (render-table-header :dataspex/idx nil sort-k sort-order opt)]
            (into (mapv #(render-table-header % % sort-k sort-order opt) ks))
            (conj [:th]))
        (into
         [::ui/tbody]
         (mapv
          (fn [[idx m]]
            (-> [::ui/tr {::ui/actions (views/navigate-to opt (views/path-to opt [idx]))}
                 (render-inline idx opt)]
                (into (mapv #(render-inline (get m %) opt) ks))
                (conj (render-copy-button opt idx))))
          (cond->> (map vector (range) xs)
            (not= sort-k :dataspex/idx)
            (sort-by (comp sort-k second))

            (= (get-in opt [:dataspex/sorting (:dataspex/path opt) :order])
               :dataspex.sort.order/descending) reverse
            :then (paginate (views/get-pagination opt)))))]))))

(defn render-source-content [data opt]
  (if (satisfies? dp/IRenderSource data)
    (dp/render-source data opt)
    (render-inline data opt)))

(defn get-ident [hiccup]
  [(first hiccup)])

(defn empty-node? [v]
  (let [len (count v)]
    (or (= 1 len) (and (= 2 len) (map? (second v))))))

(defn folded? [v {:dataspex/keys [folding-level path] :as opt} node-path]
  (let [{:keys [folded? ident]} (get-in opt [:dataspex/folding (into path node-path)])]
    (if (and ident (= ident (get-ident v)))
      folded?
      (when (or (data/hiccup? v)
                (every? data/hiccup? v))
        (< folding-level 0)))))

(declare render-hiccup-node)

(defn render-hiccup-child [node opt path idx]
  (cond
    (data/hiccup? node)
    (let [node-path (conj path idx)]
      (if (and (not (empty-node? node))
               (folded? node opt node-path))
        [::ui/vector
         {::ui/actions
          [(views/update-folding opt node-path
             {:folded? false
              :ident (get-ident node)})]}
         [::ui/hiccup-tag {:data-folded "true"} (first node)]
         [::ui/code "..."]]

        (render-hiccup-node node (update opt :dataspex/folding-level dec) node-path)))

    (list? node)
    (into
     [::ui/list]
     (let [path (conj path idx)]
       (map-indexed (fn [iidx e]
                      (render-hiccup-child e opt path iidx)) node)))

    :else
    (render-inline node opt)))

(defn render-hiccup-node [hiccup opt path]
  (let [xs (data/get-indexed-entries hiccup opt)
        [tag attrs children] (if (map? (:v (second xs)))
                               [(first xs) (second xs) (drop 2 xs)]
                               [(first xs) nil (next xs)])
        empty? (empty-node? hiccup)
        folding? (and (not empty?) (::ui/folding? opt true))
        folded? (when folding? (and (not empty?) (folded? hiccup opt path)))]
    (cond-> [::ui/vector
             (cond-> [::ui/hiccup-tag]
               folding?
               (conj {:data-folded (str folded?)
                      ::ui/actions
                      [(views/update-folding opt path
                         {:folded? (not folded?)
                          :ident (get-ident [(:v tag)])})]})
               :then (conj (:v tag)))]
      (and attrs (not folded?))
      (conj (cond-> (render-inline (:v attrs) opt)
              (< (bounded-size 21 (:v tag)) 20)
              (add-attr ::ui/inline? true)))

      (and (seq children) (not folded?))
      (into (map-indexed
             (fn [idx {:keys [v]}]
               (render-hiccup-child v opt path idx)) children))

      folded?
      (conj [::ui/code "..."]))))

(defn render-atom-source [r opt]
  (render-source-content [(deref r)] (assoc opt ::ui/prefix "#atom")))

(defn render-inline [data & [opt]]
  (if (satisfies? dp/IRenderInline data)
    (dp/render-inline data opt)
    (render-inline-object data opt)))

(defn render-dictionary [data & [opt]]
  (try
    (cond
      (satisfies? dp/IRenderDictionary data)
      (dp/render-dictionary data opt)

      (map? data)
      (render-entries-dictionary data opt (data/get-map-entries data opt))

      (coll? data)
      (render-entries-dictionary data opt (data/get-indexed-entries data opt))

      (data/js-collection? data)
      (render-entries-dictionary data opt (data/get-indexed-entries (into [] data) opt))

      (data/js-array? data)
      (render-entries-dictionary data opt (data/get-js-array-entries data opt))

      (data/js-object? data)
      (render-entries-dictionary data opt (data/get-js-object-entries data opt))

      (data/derefable? data)
      (render-dictionary (deref data) opt))
    (catch #?(:clj Exception :cljs :default) e
      [::ui/code #?(:clj (.getMessage e) :cljs (.-message e))])))

(defn render-table [data opt]
  (cond
    (satisfies? dp/IRenderTable data)
    (dp/render-table data opt)

    (data/tableable? data opt)
    (render-map-table data opt)))

(defn render-hiccup [hiccup opt]
  (let [opts (dissoc opt ::ui/prefix)]
    (cond-> [::ui/hiccup]
      (or (::ui/prefix opt)
          (::ui/inline? opt)) (conj (select-keys opt [::ui/prefix ::ui/inline?]))
      :then (conj (if (satisfies? dp/IRenderHiccup hiccup)
                    (dp/render-hiccup hiccup opts)
                    (render-hiccup-node hiccup (assoc opts :dataspex/folding-level 2) [0]))))))

(defn render-source [data opt]
  (let [opt (assoc opt :dataspex/summarize-above-w -1)]
    (if (data/hiccup? data)
      (render-hiccup data opt)
      [::ui/source
       (render-source-content data opt)])))

(defn render-inline-hiccup [hiccup opt]
  (if (summarize? hiccup opt)
    [::ui/hiccup
     (cond-> [::ui/vector {}
              [::ui/hiccup-tag (first hiccup)]]
       (map? (second hiccup))
       (conj (render-inline (second hiccup) opt))
       :then (conj [::ui/code "..."]))]
    (render-hiccup hiccup opt)))

(defn render-inline-vector [v opt]
  (if (and (data/hiccup? v) (:dataspex/hiccup? opt true))
    (render-inline-hiccup v opt)
    (->> {:get-entries data/get-indexed-entries}
         (render-paginated-sequential ::ui/vector v opt))))

(extend-type #?(:cljs string
                :clj java.lang.String)
  dp/IRenderInline
  (render-inline [s _]
    [::ui/string s])

  dp/IRenderDictionary
  (render-dictionary [s opt]
    (render-primitive-dictionary "String" s opt)))

(extend-type #?(:cljs cljs.core/Keyword
                :clj clojure.lang.Keyword)
  dp/IRenderInline
  (render-inline [k opt]
    (if-let [alias (get-in opt [:dataspex/ns-aliases (namespace k)])]
      [::ui/keyword (str ":" alias) (name k)]
      [::ui/keyword k]))

  dp/IRenderDictionary
  (render-dictionary [k opt]
    (render-primitive-dictionary "Keyword" k opt)))

(extend-type #?(:cljs number
                :clj java.lang.Number)
  dp/IRenderInline
  (render-inline [n _]
    [::ui/number n])

  dp/IRenderDictionary
  (render-dictionary [n opt]
    (render-primitive-dictionary "Number" n opt)))

(extend-type #?(:cljs boolean
                :clj java.lang.Boolean)
  dp/IRenderInline
  (render-inline [b _]
    [::ui/boolean b])

  dp/IRenderDictionary
  (render-dictionary [b opt]
    (render-primitive-dictionary "Boolean" b opt)))

(extend-type #?(:cljs cljs.core/Symbol
                :clj clojure.lang.Symbol)
  dp/IRenderInline
  (render-inline [s opt]
    (if-let [alias (get-in opt [:dataspex/ns-aliases (namespace s)])]
      [::ui/symbol alias (name s)]
      [::ui/symbol s]))

  dp/IRenderDictionary
  (render-dictionary [s opt]
    (render-primitive-dictionary "Symbol" s opt)))

(extend-type #?(:cljs cljs.core/PersistentVector
                :clj clojure.lang.PersistentVector)
  dp/IRenderInline
  (render-inline [v opt]
    (render-inline-vector v opt)))

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
    (render-inline-set s opt))

  dp/IRenderDictionary
  (render-dictionary [s opt]
    (render-entries-dictionary s opt (data/get-set-entries s opt))))

(extend-type #?(:cljs cljs.core/Atom
                :clj clojure.lang.IAtom)
  dp/IRenderInline
  (render-inline [r opt]
    (render-inline-atom r opt))

  dp/IRenderDictionary
  (render-dictionary [r opt]
    (render-dictionary (deref r) opt))

  dp/IRenderSource
  (render-source [r opt]
    (render-atom-source r opt)))

#?(:cljs
   (extend-type js/Date
     dp/IRenderDictionary
     (render-dictionary [d opt]
       (let [m (date/->map d)]
         (render-entries-dictionary m opt (data/get-map-entries m opt {:ks date/date-keys}))))))

#?(:cljs
   (when (exists? js/Element)
     (extend-type js/Element
       dp/IRenderInline
       (render-inline [el opt]
         (-> (element/->hiccup el)
             (render-hiccup (assoc opt
                                   ::ui/prefix (get-js-prefix el)
                                   ::ui/folding? false
                                   ::ui/inline? true)))))))

#?(:cljs
   (when (exists? js/Text)
     (extend-type js/Text
       dp/IRenderInline
       (render-inline [el _]
         [::ui/string (.-nodeValue el)]))))

#?(:cljs
   (when (exists? js/Event)
     (extend-type js/Event
       dp/IRenderInline
       (render-inline [event opt]
         (render-inline-map
          event
          [{:k :type
            :label :type
            :v (.-type event)}
           {:k :target
            :label :target
            :v (.-target event)}]
          (assoc opt ::ui/prefix (get-js-prefix event)))))))

#?(:cljs
   (when (exists? js/CSSStyleValue)
     (extend-type js/CSSStyleValue
       dp/IRenderInline
       (render-inline [v _]
         (if (instance? js/CSSNumericValue v)
           [::ui/number (str v)]
           [::ui/string (str v)])))))
