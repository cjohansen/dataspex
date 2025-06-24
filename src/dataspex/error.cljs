(ns dataspex.error
  (:require [clojure.string :as str]
            [dataspex.data :as data]
            [dataspex.hiccup :as hiccup]
            [dataspex.protocols :as dp]
            [dataspex.ui :as-alias ui]))

(defn get-error-entries [err opt]
  (->> [{:k :message
         :label :message
         :v (.-message err)}
        (when-let [data (ex-data opt)]
          {:k :ex-data
           :label :ex-data
           :v data})
        {:k :stacktrace
         :label :stacktrace
         :v (hiccup/preformatted-string (.-stack err))}
        (when-let [cause (.-cause err)]
          {:k :cause
           :label :cause
           :v cause})]
       (remove nil?)))

(defn get-type [err]
  (str/replace (data/get-js-constructor err) #"^cljs\$core\$" ""))

(extend-type js/Error
  dp/IRenderInline
  (render-inline [e _]
    [::ui/string {::ui/prefix (get-type e)}
     (.-message e)])

  dp/IRenderDictionary
  (render-dictionary [e opt]
    (->> (get-error-entries e opt)
         (cons {:label (hiccup/string-label "Type")
                :v (hiccup/string-label (get-type e))})
         (hiccup/render-entries-dictionary e opt)))

  dp/INavigatable
  (nav-in [ex [k & ks]]
    (data/nav-in
     (case k
       :message (.-message ex)
       :ex-data (ex-data ex)
       :stacktrace (.-stack ex)
       :cause (.-cause ex))
     ks)))
