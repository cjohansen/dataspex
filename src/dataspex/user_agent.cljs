(ns dataspex.user-agent
  (:require [clojure.string :as str]))

(defn parse-user-agent
  ([] (parse-user-agent (.-userAgent js/navigator)))
  ([user-agent-str]
   (let [ua (str/lower-case user-agent-str)]
     {:browser (cond
                 (re-find #"edg/" ua) "Edge"
                 (re-find #"chrome/" ua) "Chrome"
                 (re-find #"safari/" ua) "Safari"
                 (re-find #"firefox/" ua) "Firefox"
                 (re-find #"msie|trident/" ua) "Internet Explorer"
                 :else "Unknown")

      :version (or
                (some->> (re-find #"(?:chrome|firefox|version|edg)/([\d\.]+)" ua)
                         second)
                "Unknown")

      :os (cond
            (re-find #"windows nt" ua) "Windows"
            (re-find #"mac os x" ua) "macOS"
            (re-find #"android" ua) "Android"
            (re-find #"iphone|ipad|ipod" ua) "iOS"
            (re-find #"linux" ua) "Linux"
            :else "Unknown")})))
