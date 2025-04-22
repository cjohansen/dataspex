(ns dataspex.dev
  (:require [dataspex.core :as dataspex]
            [dataspex.data :as data]
            [dataspex.datascript :as datascript]
            [dataspex.demo-data :as demo-data]
            [dataspex.in-process-channel :as in-process-channel]
            [dataspex.inspector :as inspector]
            [dataspex.jwt :as jwt]
            [dataspex.render-client :as rc]))

::datascript/keep

(defonce app-state
  {:app/title "Movie Explorer"
   :user {:id 42
          :name "Ada Lovelace"
          :roles #{:admin :reviewer}
          :auth-token "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjQyfQ.sEcrEt-KU5phTcLBBjDDxpsHloA4j1ebJfXnB4rmkmc"
          :last-login (js/Date. "2025-04-15T12:34:56Z")}

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
           :raw-js-object (js-obj "a" 1 "b" (clj->js {:nested [1 2 3]}))}})

(defonce store (atom {}))
(data/add-string-inspector! jwt/inspect-jwt)

(defonce txes
  (demo-data/add-data demo-data/conn))

(rc/start-render-client
 {:channels {:process (in-process-channel/create-channel store)}})

(defn ^:dev/after-load main []
  (swap! store assoc ::loaded (.getTime (js/Date.))))

(dataspex/inspect "App state" app-state)
(dataspex/inspect "DB" demo-data/conn)

(inspector/inspect store "App state" (assoc app-state :stuff "Magnar"))
(inspector/inspect store "DB" demo-data/conn)
