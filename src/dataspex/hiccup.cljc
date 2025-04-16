(ns dataspex.hiccup
  (:require [dataspex.data :as data]
            [dataspex.protocols :as dp]
            [dataspex.ui :as-alias ui]))

(defn render-inline-object [o _]
  (let [string (data/stringify o)]
    (if-let [[_ prefix s] (re-find #"(#[a-zA-Z_\-*+!?=<>][a-zA-Z0-9_\-*+!?=<>/.]+)\s(.+)$" string)]
      [::ui/literal {::ui/prefix prefix}
       (if-let [[_ s] (re-find #"^\"(.*)\"$" s)]
         [::ui/string s]
         [::ui/code s])]
      [::ui/code string])))

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

(defn render-inline [data & [opt]]
  (if (satisfies? dp/IRenderInline data)
    (dp/render-inline data opt)
    (render-inline-object data opt)))
