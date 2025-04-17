(ns dataspex.time
  #?(:clj (:import [java.time ZoneId ZonedDateTime]
                   [java.time.format DateTimeFormatter])))

(defn get-default-timezone []
  #?(:clj (ZoneId/systemDefault)))

(defn hh:mm:ss [inst]
  (when inst
    #?(:clj
       (-> (DateTimeFormatter/ofPattern "HH:mm:ss")
           (.format (-> (.toInstant inst)
                        (ZonedDateTime/ofInstant (get-default-timezone)))))

       :cljs
       (-> (js/Intl.DateTimeFormat. "default"
                                    #js {:hour "2-digit"
                                         :minute "2-digit"
                                         :second "2-digit"
                                         :hour12 false})
           (.format inst)))))

(defn now []
  #?(:clj (java.util.Date.)
     :cljs (js/Date.)))
