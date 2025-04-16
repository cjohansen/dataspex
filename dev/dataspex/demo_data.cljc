(ns dataspex.demo-data
  (:require [datascript.core :as d]))

(def schema
  {:user/id {:db/unique :db.unique/identity}
   :user/name {}
   :user/roles {:db/cardinality :db.cardinality/many}
   :user/reviews {:db/valueType :db.type/ref
                  :db/cardinality :db.cardinality/many}

   :movie/id {:db/unique :db.unique/identity}
   :movie/title {}
   :movie/year {}
   :movie/genres {:db/cardinality :db.cardinality/many}
   :movie/cast {:db/cardinality :db.cardinality/many}
   :movie/reviews {:db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/many}

   :review/id {:db/unique :db.unique/identity}
   :review/rating {}
   :review/comment {}
   :review/user {:db/valueType :db.type/ref}
   :review/movie {:db/valueType :db.type/ref}
   :review/created-at {}})

(def data
  (concat
   [{:user/id 1
     :user/name "Ada Lovelace"
     :user/roles #{:admin :reviewer}}

    {:user/id 2
     :user/name "Alan Turing"
     :user/roles #{:reviewer}}

    {:user/id 3
     :user/name "Grace Hopper"
     :user/roles #{:moderator}}

    {:movie/id 101
     :movie/title "Interstellar"
     :movie/year 2014
     :movie/genres #{:sci-fi :drama}
     :movie/cast ["Matthew McConaughey" "Anne Hathaway"]}

    {:movie/id 102
     :movie/title "Amélie"
     :movie/year 2001
     :movie/genres #{:comedy :romance}
     :movie/cast ["Audrey Tautou"]}

    {:movie/id 103
     :movie/title "Arrival"
     :movie/year 2016
     :movie/genres #{:sci-fi :thriller}
     :movie/cast ["Amy Adams"]}]

   (map-indexed
    (fn [i {:keys [user-id movie-id rating comment]}]
      {:review/id (+ 1000 i)
       :review/user [:user/id user-id]
       :review/movie [:movie/id movie-id]
       :review/rating rating
       :review/comment comment
       ;;:review/created-at (js/Date. (+ 1600000000000 (* i 1000000)))
       })
    [{:user-id 1 :movie-id 101 :rating 9.0 :comment "Mind-blowing"}
     {:user-id 2 :movie-id 101 :rating 8.5 :comment "Great visuals"}
     {:user-id 1 :movie-id 102 :rating 7.8 :comment "Very sweet"}
     {:user-id 3 :movie-id 103 :rating 9.2 :comment "Intense and thought-provoking"}])))

(defonce conn
  (let [conn (d/create-conn schema)]
    (d/transact! conn data)
    conn))
