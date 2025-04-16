(ns dataspex.dev
  (:require [dataspex.actions :as actions]
            [dataspex.client.local :as local-client]
            [dataspex.data :as data]
            [dataspex.datascript :as datascript]
            [dataspex.demo-data :as demo-data]
            [dataspex.jwt :as jwt]
            [dataspex.panel :as panel]
            [dataspex.protocols :as dp]))

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

   :movies [{:id 101
             :title "Interstellar"
             :year 2014
             :genres [:sci-fi :drama]
             :rating 8.6
             :cast ["Matthew McConaughey" "Anne Hathaway"]
             :meta {:favorited? true
                    :watchlist? false}}

            {:id 102
             :title "Amélie"
             :year 2001
             :genres [:comedy :romance]
             :rating 8.3
             :cast ["Audrey Tautou"]
             :meta {:favorited? false
                    :watchlist? true}}]

   :debug {:expanded-paths #{[:movies 0 :cast]
                             [:ui :modal]}
           :watch-history (mapv (fn [i] {:ts (+ 1713200000 i)
                                         :value (rand-nth [true false nil])})
                                (range 20))
           :raw-js-array (js/Array. 1 2 3 "four" true {:x 5})
           :raw-js-object (js-obj "a" 1 "b" (clj->js {:nested [1 2 3]}))}})

(defonce client (local-client/create-client js/document.body))
(defonce store (atom {"App state" {:dataspex/pagination {:page-size 100}
                                   :dataspex/path []}
                      "Datascript" {:dataspex/path []}}))

(data/add-string-inspector! jwt/inspect-jwt)

(dp/set-action-handler
 client
 (fn [actions]
   (prn 'Actions actions)
   (actions/handle-actions store actions)))

(defn render [state]
  (dp/render client
   [:div
    (panel/render-panel state "App state" app-state)
    (panel/render-panel state "Datascript" demo-data/conn)]))

(add-watch store ::render (fn [_ _ _ state] (render state)))

(defn ^:dev/after-load main []
  (swap! store assoc ::loaded (.getTime (js/Date.))))
