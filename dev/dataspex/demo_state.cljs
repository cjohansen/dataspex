(ns dataspex.demo-state)

(defonce app-state
  {:app/title "Movie Explorer"
   :really.long.ns/title "I'm a keyword with a long namespace"
   :user {:id 42
          :name "Ada Lovelace"
          :roles #{:admin :reviewer}
          :auth-token "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjQyfQ.sEcrEt-KU5phTcLBBjDDxpsHloA4j1ebJfXnB4rmkmc"
          :last-login (js/Date. "2025-04-15T12:34:56Z")
          :really.long.ns/secret "Shush!"}

   :filters {:genres [:drama :sci-fi :comedy]
             :year-range {:min 1990 :max 2024}
             :search-term ""}

   :ui {:modal {:type :movie/details
                :movie-id 101}
        :notifications [{:id 1 :type :info :message "Welcome, Ada!"}
                        {:id 2 :type :error :message "Could not fetch recommendations"}]
        :pagination {:current 2 :per-page 10}}

   :movies [(with-meta
              {:id 101
               :title "Interstellar"
               :year 2014
               :genres [:sci-fi :drama]
               :rating 8.6
               :cast ["Matthew McConaughey" "Anne Hathaway"]}
              {:favorited? true
               :watchlist? false})

            (with-meta
              {:id 102
               :title "Amélie"
               :year 2001
               :genres [:comedy :romance]
               :rating 8.3
               :cast ["Audrey Tautou"]}
              {:favorited? false
               :watchlist? true})]

   :debug {:expanded-paths #{[:movies 0 :cast]
                             [:ui :modal]}
           :watch-history (mapv (fn [i] {:ts (+ 1713200000 i)
                                         :value (rand-nth [true false nil])})
                                (range 20))
           :raw-js-array (js/Array. 1 2 3 "four" true {:x 5})
           :raw-js-object (js-obj "a" 1 "b" (clj->js {:nested [1 2 3]}))}

   :token "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30"})
