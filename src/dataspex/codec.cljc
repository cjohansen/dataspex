(ns dataspex.codec
  (:require [clojure.string :as str]
            #?(:cljs [cljs.reader :as reader])))

(defn parse-string [data-string]
  (try
    (if (empty? data-string)
      nil
      #?(:clj (read-string data-string)
         :cljs (reader/read-string data-string)))
    (catch #?(:clj Exception :cljs :default) _
      ;; Possibly we have an unreadable keyword? If it's a value, we might be
      ;; able to salvage the wreck.
      (-> (str/replace
           data-string #":(\:[^\s/\]]+)(?:/([^\s/\]]+))"
           (fn [[_ a b]]
             (str "\"" a "\""
                  (when b
                    (str " \"" b "\"")))))
          #?(:clj read-string
             :cljs reader/read-string)))))

(defn generate-string [data]
  (pr-str data))
