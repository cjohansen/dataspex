(ns dataspex.json)

(defn parse-string [s]
  (-> s js/JSON.parse (js->clj :keywordize-keys true)))
