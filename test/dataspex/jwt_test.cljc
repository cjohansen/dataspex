(ns dataspex.jwt-test
  (:require [clojure.test :refer [deftest is testing]]
            [dataspex.data :as data]
            [dataspex.helper :as h]
            [dataspex.jwt :as jwt]
            [dataspex.ui :as-alias ui]
            [lookup.core :as lookup]))

(def token "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")

(data/add-string-inspector! jwt/inspect-jwt)

(deftest jwt-test
  (testing "Parses JWT"
    (is (= (update (into {} (jwt/inspect-jwt token)) :token :token)
           {:token token
            :headers {:alg "HS256"
                      :typ "JWT"}
            :data {:sub "1234567890"
                   :name "John Doe"
                   :iat 1516239022}
            :sig "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"})))

  (testing "Returns nil for non-JWT"
    (is (nil? (jwt/inspect-jwt "ey, how ya doin?"))))

  (testing "Renders JWT inline"
    (is (= (h/render-inline token)
           [::ui/literal {::ui/prefix "JWT"}
            [::ui/string "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."]])))

  (testing "Renders JWT in dictionary browser"
    (is (= (->> (h/render-dictionary token)
                (lookup/select '[::ui/entry > ::ui/keyword])
                (mapcat lookup/children))
           [:headers :data :sig :token])))

  (testing "Navigates in JWT"
    (is (= (-> (data/inspect token {})
               (data/nav-in [:data]))
           {:sub "1234567890"
            :name "John Doe"
            :iat 1516239022})))

  (testing "Navigates deeply in JWT"
    (is (= (-> (data/inspect {:token token} {})
               (data/nav-in [:token :data]))
           {:sub "1234567890"
            :name "John Doe"
            :iat 1516239022})))

  (testing "Doesn't eternally nest token"
    (is (= (-> (data/inspect {:token token} {})
               (data/nav-in [:token :token])
               data/inspect)
           token))))
