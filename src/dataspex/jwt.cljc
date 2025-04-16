(ns dataspex.jwt
  (:require #?(:clj [clojure.data.json :as json])
            [clojure.core.protocols]
            [clojure.string :as str]
            [dataspex.data :as data]
            [dataspex.hiccup :as hiccup]
            [dataspex.protocols :as dp]
            [dataspex.ui :as-alias ui])
  #?(:clj (:import (java.util Base64))))

(def re-jwt #"^[A-Za-z0-9-_=]{4,}\.[A-Za-z0-9-_=]{4,}\.?[A-Za-z0-9-_.+/=]*$")

(defn render-jwt-dictionary [jwt opt]
  (hiccup/render-entries-dictionary
   jwt
   (data/get-map-entries jwt opt {:ks [:headers :data :sig :token]})
   opt))

(defrecord TokenString [token]
  clojure.core.protocols/Datafiable
  (datafy [_]
    token))

(defrecord JWT [token headers data sig]
  dp/IRenderInline
  (render-inline [jwt _]
    [::ui/literal {::ui/prefix "#jwt"}
     [::ui/string (str (first (str/split (:token (:token jwt)) #"\.")) "...")]])

  dp/IRenderDictionary
  (render-dictionary [jwt opt]
    (render-jwt-dictionary jwt opt)))

(defn base64-url-decode [^String s]
  #?(:clj (String. (.decode (Base64/getUrlDecoder) s))
     :cljs (js/atob s)))

(defn parse-json [s]
  #?(:cljs (-> s js/JSON.parse (js->clj :keywordize-keys true))
     :clj (json/read-str s :key-fn keyword)))

(defn unpack [s]
  (-> s base64-url-decode parse-json))

(defn parse-jwt [token]
  (let [[header data sig] (str/split token #"\.")]
    (JWT. (TokenString. token) (unpack header) (unpack data) sig)))

(defn inspect-jwt [s]
  (when (re-find re-jwt s)
    (try
      (parse-jwt s)
      (catch #?(:cljs :default :clj Exception) _
        nil))))
