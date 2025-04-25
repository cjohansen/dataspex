(ns dataspex.ui
  (:require [dataspex.data :as data]
            [replicant.alias :refer [defalias]])
  (:refer-clojure :exclude [keyword boolean symbol vector set map list]))

(defn actions->click-handler [attrs]
  (cond-> (or attrs {})
    (::actions attrs) (assoc-in [:on :click] (::actions attrs))
    (::actions attrs) (assoc :class #{:clickable})))

(defalias string [attrs [string]]
  [:code (assoc (actions->click-handler attrs) :data-type "string")
   (str "\"" string "\"")])

(defalias number [attrs [number]]
  [:code (assoc attrs :data-type "number") (str number)])

(defn render-named [named & [prefix]]
  (if-let [ns (namespace named)]
    [[:span.namespace (str prefix ns)] "/"
     [:span.name (name named)]]
    [[:span.name (str prefix (name named))]]))

(defalias keyword [attrs [kw]]
  (into
   [:code (assoc (actions->click-handler attrs) :data-type "keyword")]
   (if (keyword? kw)
     (render-named kw ":")
     [[:span.name (str kw)]])))

(defalias boolean [attrs [boolean]]
  [:code (assoc (actions->click-handler attrs) :data-type "boolean")
   (str boolean)])

(defalias symbol [attrs [symbol]]
  (into
   [:code (assoc (actions->click-handler attrs) :data-type "symbol")]
   (if (symbol? symbol)
     (render-named symbol)
     [(str symbol)])))

(defalias code [attrs value]
  [:code.code (actions->click-handler attrs) value])

(defalias literal [attrs value]
  [:span (actions->click-handler attrs)
   [:code.code.strong (str (::prefix attrs) " ")]
   value])

(defn parse-tag [^clojure.lang.Keyword tag]
  ;; Borrowed from hiccup, and adapted to support multiple classes
  (let [ns ^String (namespace tag)
        tag ^String (name tag)
        id-index (let [index (.indexOf tag "#")] (when (pos? index) index))
        class-index (let [index (.indexOf tag ".")] (when (pos? index) index))
        tag-name (cond
                   id-index (.substring tag 0 id-index)
                   class-index (.substring tag 0 class-index)
                   :else tag)
        id (when id-index
             (if class-index
               (.substring tag (unchecked-inc-int id-index) class-index)
               (.substring tag (unchecked-inc-int id-index))))
        classes (when class-index
                  (seq (.split (.substring tag (unchecked-inc-int class-index)) #?(:clj "\\." :cljs "."))))]
    [ns tag-name id classes]))

(defalias hiccup-tag [attrs [tag]]
  (let [[ns tag id classes] (parse-tag tag)]
    (into
     [:code.hiccup-tag (assoc (actions->click-handler attrs) :data-type "keyword")]
     (cond-> (if ns
               [[:span.namespace (str ":" ns)] "/"
                [:span.name (name tag)]]
               [[:span.name (str ":" (name tag))]])
       id (conj [:span.hiccup-id (str "#" id)])
       classes (into (mapv (fn [class]
                             [:span.hiccup-class
                              (str "." class)]) classes))))))

(defn render-collection [left-bracket right-bracket attrs elements]
  [:span.coll (actions->click-handler attrs)
   [:code.code.strong
    (when-let [prefix (::prefix attrs)]
      (str prefix " "))
    left-bracket]
   (interpose " " elements)
   [:code.code.strong right-bracket]])

(defalias vector [attrs elements]
  (render-collection "[" "]" attrs elements))

(defalias set [attrs elements]
  (render-collection "#{" "}" attrs elements))

(defalias list [attrs elements]
  (render-collection "(" ")" attrs elements))

(defalias map [attrs elements]
  [:span (actions->click-handler attrs)
   [:code.code.strong
    (str (when-let [prefix (::prefix attrs)]
           (str prefix " "))) "{"]
   (->> (for [kv elements]
          (interpose " " (drop 1 kv)))
        (interpose [:strong ", "]))
   [:code.code.strong "}"]])

(defalias inline-tuple [{::keys [prefix] :as attrs} values]
  [:span.tuple attrs
   (when prefix
     [:code.code [:strong prefix " "]])
   [:code.code.strong "["]
   (for [value values]
     (let [actions (when (data/hiccup? value)
                     (-> value second ::actions))]
       [(if actions :a.tuple-item.clickable :span.tuple-item)
        {:on {:click actions}}
        value]))
   [:code.code.strong "]"]])

(defalias tuple [{::keys [actions prefix] :as attrs} values]
  [(if actions :tr.clickable :tr)
   (assoc attrs :on {:click actions})
   [:th.no-padding
    (when prefix
      [:code.code [:strong prefix " "]])
    [:code.code.strong "["]]
   (let [last-idx (dec (count values))]
     (map-indexed
      (fn [idx value]
        (let [actions (when (data/hiccup? value)
                        (-> value second ::actions))]
          [(if actions :td.clickable :td)
           {:on {:click actions}
            :class (when (= idx last-idx)
                     :no-padding)}
           value]))
      values))
   [:td [:code.code.strong "]"]]])

(defalias entry [{::keys [actions]} [k v button]]
  [(if actions :tr.clickable :tr) {:on {:click actions}}
   [:th k]
   [:td [:span.flex v button]]])

(defalias dictionary [attrs entries]
  [:table.table.dictionary attrs
   [:tbody entries]])

(defalias th [{::keys [actions] :as attrs} content]
  [(if actions :th.clickable :th)
   (cond-> attrs
     actions (assoc-in [:on :click] actions))
   content])

(defalias thead [attrs ths]
  [:thead attrs
   [:tr
    (for [th ths]
      (if (#{:th ::th} (first th))
        th
        [:th th]))]])

(defalias tbody [attrs rows]
  [:tbody attrs rows])

(defalias tr [{::keys [actions] :as attrs} tds]
  [(if actions :tr.clickable :tr)
   (cond-> attrs
     actions (assoc-in [:on :click] actions))
   (for [td tds]
     [:td td])])

(defalias table [attrs sections]
  [:table.table attrs
   sections])

(defn indent-str [n]
  (loop [s ""
         n n]
    (if (= 0 n)
      s
      (recur (str s " ") (dec n)))))

(declare render-source-element)

(defn bounded-sum [bound numbers]
  (if bound
    (loop [sum 0
           numbers numbers]
      (if (or (< bound sum)
              (empty? numbers))
        sum
        (recur (+ sum (first numbers)) (next numbers))))
    (reduce + 0 numbers)))

(defn content-length [node & [bound]]
  (let [[tag & xs] node
        [attrs children] (if (map? (first xs))
                           [(first xs) (drop 1 xs)]
                           [nil xs])]
    (+ (if-let [prefix (::prefix attrs)]
         (inc (count prefix))
         0)
       (case tag
         ::string
         (+ 2 (count (str (first children))))

         (::vector ::list ::set)
         (+ (if (= ::set tag) 1 0)
            2                      ;; Brackets
            (dec (count children)) ;; Spaces
            (->> (mapv content-length children)
                 (bounded-sum bound)))

         ::map
         (+ 2                            ;; Brackets
            (* 2 (dec (count children))) ;; Commas + spaces
            (->> (mapv (fn [[_ k v]]
                         (+ (content-length k)
                            1 ;; Space
                            (content-length v))) children)
                 (bounded-sum bound)))

         (count (str (first children)))))))

(defn render-map-source [{::keys [prefix] :as attrs} map-entries indent]
  (let [indent-s (indent-str (+ indent 1 (if prefix (inc (count prefix)) 0)))]
    (into [:span attrs
           [::code.strong
            (when-let [prefix prefix]
              (str prefix " ")) "{"]]
          (loop [entries map-entries
                 res []]
            (let [[_ k v] (first entries)
                  indent-w (+ indent 2 (content-length k))
                  new-line? (and (< 40 indent-w)
                                 (#{::map ::list ::set ::vector} (first v))
                                 (< 60 (content-length v 60)))
                  more (next entries)
                  res (cond-> res
                        v (into [k [::code (if new-line? (str "\n" indent-s) " ")]
                                 (render-source-element v
                                   {:indent (if new-line?
                                              (inc indent)
                                              (+ indent 2 (content-length k)))})])
                        more (conj (str "\n" indent-s))
                        (nil? more) (conj [::code.strong "}"]))]
              (if more
                (recur more res)
                res))))))

(defn render-coll-source [{::keys [prefix] :as attrs} xs indent l-br r-br]
  (let [indent (+ indent (count l-br) (if prefix (inc (count prefix)) 0))
        indent-s (indent-str (+ indent (if prefix (inc (count prefix)) 0)))
        inline? (< (reduce + indent (mapv content-length xs)) 80)
        separator (if inline?
                    " "
                    (str "\n" indent-s))]
    (into [:span.coll (actions->click-handler attrs)
           [::code.strong
            (str (when prefix
                   (str prefix " ")) l-br)]]
          (loop [values xs
                 res []
                 column indent
                 prev nil]
            (let [v (first values)
                  more (next values)
                  sep (if (::inline? (second (first more)))
                        " "
                        separator)
                  res (cond-> (conj res (render-source-element v
                                          {:indent
                                           (if (or inline? (and (::inline? (second v)) prev))
                                             column
                                             indent)}))
                        more (conj sep)
                        (nil? more) (conj [::code.strong r-br]))]
              (if more
                (recur more res (+ column 1 (content-length v)) v)
                res))))))

(defn ^{:indent 1} render-source-element [element {:keys [indent]}]
  (let [[kind & xs] element
        [attrs children] (if (map? (first xs))
                           [(first xs) (rest xs)]
                           [nil xs])]
    (case kind
      ::map
      (render-map-source attrs children indent)

      ::vector
      (render-coll-source attrs children indent "[" "]")

      ::list
      (render-coll-source attrs children indent "(" ")")

      ::set
      (render-coll-source attrs children indent "#{" "}")

      element)))

(defalias source [attrs elements]
  (let [indent (if (::prefix attrs) (inc (count (::prefix attrs))) 0)]
    (cond-> [:pre.source attrs]
      (::prefix attrs) (conj [:code.strong (str (::prefix attrs) " ")])
      :then (into (mapv #(render-source-element % {:indent indent}) elements)))))

(defalias hiccup [attrs elements]
  (into [:pre.source.hiccup attrs]
        (mapv #(render-source-element % {:indent 0}) elements)))

(defalias tag [attrs value]
  [:code.tag attrs value])

(defalias alert [attrs content]
  [:output.alert attrs
   content])

(defalias enumeration [attrs content]
  (into [:span attrs]
        (interpose ", " content)))

(defalias card [attrs content]
  [:article.card attrs content])

(defalias card-header [attrs content]
  [:div.card-header
   (actions->click-handler attrs)
   content])

(defalias card-body [attrs content]
  [:section.card-body attrs
   content])

(defalias card-list [attrs content]
  [:nav.card-list attrs
   content])

(defalias timestamp [attrs content]
  [:time.tag attrs content])

(defalias success [attrs content]
  [:strong (assoc attrs :data-color "success") content])

(defalias error [attrs content]
  [:strong (assoc attrs :data-color "error") content])

(defalias link [attrs text]
  [:button.link
   (cond-> attrs
     (::actions attrs) (assoc-in [:on :click] (::actions attrs)))
   [:code.code text]])

(defalias clickable [attrs text]
  [:button.clickable
   (cond-> attrs
     (::actions attrs) (assoc-in [:on :click] (::actions attrs)))
   text])

(defalias button [{::keys [title actions selected?] :as attrs} content]
  [:button.button
   (into (update attrs :class conj (cond
                                     selected? :selected
                                     (nil? actions) :disabled))
         {:on {:click actions}
          :title title})
   content])

(defalias crumb [{::keys [actions] :as attrs} content]
  [(if actions
     :button.clickable.pill.pill-big
     :span.pill.pill-ph)
   (assoc attrs :on {:click actions})
   (if (data/hiccup? (first content))
     content
     [:code.code content])])

(defalias path [attrs crumbs]
  [:nav attrs crumbs])

(defalias navbar [attrs children]
  [:div.flex.navbar attrs children])

(defalias button-bar [attrs children]
  [:nav.flex.gap-2 attrs children])

(defalias tab [{::keys [selected? actions] :as attrs} content]
  [(if actions :button.tab.clickable :div.tab)
   (cond-> (assoc attrs :on {:click actions})
     selected? (update :class conj :tab-selected))
   content])

(defalias tabs [attrs children]
  [:nav.flex attrs children])

(defalias toolbar [attrs children]
  [:div.toolbar.flex attrs
   children])
