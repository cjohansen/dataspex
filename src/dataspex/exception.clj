(ns dataspex.exception
  (:require [clojure.string :as str]
            [dataspex.data :as data]
            [dataspex.hiccup :as hiccup]
            [dataspex.protocols :as dp]
            [dataspex.ui :as-alias ui]))

(defn get-type [exception]
  (-> exception
      .getClass
      .getName
      (str/replace #"^(?:java|clojure)\.lang\." "")))

(defn get-stack-trace-str [^Exception err]
  (let [sw (java.io.StringWriter.)
        pw (java.io.PrintWriter. sw)]
    (.printStackTrace err pw)
    (hiccup/preformatted-string (str sw))))

(defn get-exception-entries [err opt]
  (->> [{:k :message
         :label :message
         :v (.getMessage err)}
        (when-let [data (ex-data opt)]
          {:k :ex-data
           :label :ex-data
           :v data})
        {:k :stacktrace
         :label :stacktrace
         :v (get-stack-trace-str err)}
        (when-let [cause (.getCause err)]
          {:k :cause
           :label :cause
           :v cause})]
       (remove nil?)))

(extend-type Exception
  dp/IRenderInline
  (render-inline [e _]
    [::ui/string {::ui/prefix (get-type e)}
     (.getMessage e)])

  dp/IRenderDictionary
  (render-dictionary [e opt]
    (->> (get-exception-entries e opt)
         (cons {:label (hiccup/string-label "Type")
                :v (hiccup/string-label (get-type e))})
         (hiccup/render-entries-dictionary e opt)))

  dp/INavigatable
  (nav-in [ex [k & ks]]
    (data/nav-in
     (case k
       :message (.getMessage ex)
       :ex-data (ex-data ex)
       :stacktrace (get-stack-trace-str ex)
       :cause (.getCause ex))
     ks)))
