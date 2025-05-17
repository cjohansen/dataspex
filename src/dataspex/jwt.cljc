(ns dataspex.jwt
  (:require [clojure.core.protocols]
            [clojure.string :as str]
            [dataspex.data :as data]
            [dataspex.hiccup :as hiccup]
            [dataspex.json :as json]
            [dataspex.protocols :as dp]
            [dataspex.ui :as-alias ui])
  #?(:clj (:import (java.util Base64))))

(def re-jwt #"^[A-Za-z0-9-_=]{4,}\.[A-Za-z0-9-_=]{4,}\.?[A-Za-z0-9-_.+/=]*$")

(defn render-jwt-dictionary [jwt opt]
  (->> (data/get-map-entries jwt opt {:ks [:headers :data :sig :token]})
       (hiccup/render-entries-dictionary jwt opt)))

(defrecord TokenString [token]
  clojure.core.protocols/Datafiable
  (datafy [_]
    token))

(defrecord JWT [token headers data sig]
  dp/IRenderInline
  (render-inline [jwt _]
    [::ui/literal {::ui/prefix "JWT"}
     [::ui/string (str (first (str/split (:token (:token jwt)) #"\.")) "...")]])

  dp/IRenderDictionary
  (render-dictionary [jwt opt]
    (render-jwt-dictionary jwt opt)))

(defn base64-url-decode [^String s]
  #?(:clj (String. (.decode (Base64/getUrlDecoder) s))
     :cljs (js/atob s)))

(defn unpack [s]
  (-> s base64-url-decode json/parse-string))

(defn parse-jwt [token]
  (let [[header data sig] (str/split token #"\.")]
    (JWT. (TokenString. token) (unpack header) (unpack data) sig)))

(defn inspect-jwt [s]
  (when (re-find re-jwt s)
    (try
      (parse-jwt s)
      (catch #?(:cljs :default :clj Exception) _
        nil))))
