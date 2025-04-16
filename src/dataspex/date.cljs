(ns dataspex.date)

(def supports-intl? (and js/Intl js/Intl.DateTimeFormat))
(def date-keys [:iso :locale-date-string :year :month :date :time :timezone :timestamp])

(defn pad [n]
  (cond->> (str n)
    (< n 10) (str "0")))

(defn ->map [date]
  (cond-> {:iso (.toISOString date)
           :locale-date-string (->> (clj->js {:weekday "long"
                                              :year "numeric"
                                              :month "long"
                                              :day "numeric"})
                                    (.toLocaleDateString date "en-US"))
           :year (+ 1900 (.getYear date))
           :month (inc (.getMonth date))
           :date (.getDate date)
           :time (str (pad (.getHours date)) ":" (pad (.getMinutes date)) ":" (pad (.getSeconds date)))
           :timestamp (.getTime date)}
    supports-intl? (assoc :timezone (.. js/Intl DateTimeFormat resolvedOptions -timeZone))))
