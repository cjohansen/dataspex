(ns dataspex.json)

(def ^:private json-read-fns
  [[(symbol "clojure.data.json" "read-str")
    #(requiring-resolve 'clojure.data.json/read-str)]
   [(symbol "charred.api" "read-json")
    #(requiring-resolve 'charred.api/read-json)]
   [(symbol "cheshire.core" "parse-string")
    #(requiring-resolve 'cheshire.core/parse-string)]])

(def ^:private read-json-fn
  (some (fn [[_ resolve-fn]]
          (try
            (let [f (resolve-fn)]
              (when (ifn? f) f))
            (catch Throwable _ nil)))
        json-read-fns))

(defn parse-string [s]
  (if read-json-fn
    ;; Dispatch based on which function we have resolved
    (cond
      (= read-json-fn (requiring-resolve 'clojure.data.json/read-str))
      (read-json-fn s :key-fn keyword)

      (= read-json-fn (requiring-resolve 'charred.api/read-json))
      (read-json-fn s :key-fn keyword)

      (= read-json-fn (requiring-resolve 'cheshire.core/parse-string))
      (read-json-fn s keyword))

    ;; Fallback if none are found
    nil))
