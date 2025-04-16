(ns dataspex.ui
  (:require [replicant.alias :refer [defalias]]
            [replicant.hiccup :as hiccup])
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
   [:code.code.strong (::prefix attrs)]
   " "
   value])

(defn render-collection [left-bracket right-bracket attrs elements]
  [:span.coll (actions->click-handler attrs)
   [:strong
    (when-let [prefix (::prefix attrs)]
      (str prefix " "))
    left-bracket]
   (interpose " " elements)
   [:strong right-bracket]])

(defalias vector [attrs elements]
  (render-collection "[" "]" attrs elements))

(defalias set [attrs elements]
  (render-collection "#{" "}" attrs elements))

(defalias list [attrs elements]
  (render-collection "(" ")" attrs elements))

(defalias map [attrs elements]
  [:span attrs
   [:strong (when-let [prefix (::prefix attrs)]
              [::code.strong (str prefix " ")]) "{"]
   (->> (for [kv elements]
          (interpose " " (drop 1 kv)))
        (interpose [:strong ", "]))
   [:strong "}"]])

(defalias inline-tuple [{::keys [prefix] :as attrs} values]
  [:span.tuple attrs
   (when prefix
     [:code.code [:strong prefix " "]])
   [:strong "["]
   (for [value values]
     (let [actions (when (hiccup/hiccup? value)
                     (-> value second ::actions))]
       [(if actions :a.tuple-item.clickable :span.tuple-item)
        {:on {:click actions}}
        value]))
   [:strong "]"]])

(defalias tuple [{::keys [actions prefix] :as attrs} values]
  [(if actions :tr.clickable :tr)
   (assoc attrs :on {:click actions})
   [:th.no-padding
    (when prefix
      [:code.code [:strong prefix " "]])
    [:code.code "["]]
   (let [last-idx (dec (count values))]
     (map-indexed
      (fn [idx value]
        (let [actions (when (hiccup/hiccup? value)
                        (-> value second ::actions))]
          [(if actions :td.clickable :td)
           {:on {:click actions}
            :class (when (= idx last-idx)
                     :no-padding)}
           value]))
      values))
   [:td [:code.code "]"]]])

(defalias entry [{::keys [actions]} [k v button]]
  [(if actions :tr.clickable :tr) {:on {:click actions}}
   [:th k]
   [:td [:span.flex v button]]])

(defalias dictionary [attrs entries]
  [:table.table.dictionary attrs
   [:tbody entries]])

(defalias link [attrs text]
  [:button.link
   (cond-> attrs
     (::actions attrs) (assoc-in [:on :click] (::actions attrs)))
   [:code.code text]])

(defalias button [{::keys [title actions selected?] :as attrs} content]
  [:button.button
   (into (update attrs :class conj (cond
                                     selected? :selected
                                     (nil? actions) :disabled))
         {:on {:click actions}
          :title title})
   content])
