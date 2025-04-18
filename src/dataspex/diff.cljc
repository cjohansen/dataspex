(ns dataspex.diff
  (:require [clojure.datafy :as datafy]
            [dataspex.data :as data]
            [dataspex.protocols :as dp]
            [editscript.core :as e]
            [editscript.edit :as edit]))

(defn ->diffable [x]
  (if (satisfies? dp/IDiffable x)
    (dp/->diffable x)
    (datafy/datafy x)))

(defn worth-tracking? [x]
  (and (not (nil? x))
       (or (not (coll? x)) (not-empty x))))

(defn diff [a b]
  (->> (e/diff
        (->diffable a)
        (->diffable b))
       edit/get-edits
       (mapcat
        (fn [[path op v]]
          (case op
            :r (let [old-v (data/nav-in a path)]
                 (cond-> [[path :+ v]]
                   (worth-tracking? old-v)
                   (conj [path :- old-v])))
            :+ [[path op v]]
            :- [[path op (data/nav-in a path)]])))))

(defn grouping-path [path]
  (cond->> path
    (< 1 (count path))
    butlast))

(defn get-stats [edits]
  (->> (mapcat
        (fn [[_ op]]
          (case op
            :+ [:insertions]
            :- [:deletions]))
        edits)
       frequencies))

(defn summarize [edits]
  (->> edits
       (group-by (comp grouping-path first))
       (mapv
        (fn [[path xs]]
          (assoc (get-stats xs) :path path)))))
