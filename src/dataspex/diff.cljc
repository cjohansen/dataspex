(ns dataspex.diff
  (:require [clojure.datafy :as datafy]
            [dataspex.protocols :as dp]
            [editscript.core :as e]
            [editscript.edit :as edit]))

(defn ->diffable [x]
  (if (satisfies? dp/IDiffable x)
    (dp/->diffable x)
    (datafy/datafy x)))

(defn diff [a b]
  (edit/get-edits (e/diff (->diffable a) (->diffable b))))

(defn grouping-path [path]
  (cond->> path
    (< 1 (count path))
    butlast))

(def op->ks
  {:+ :insertions
   :- :deletions
   :r :replacements})

(defn get-stats [edits]
  (frequencies (mapv (comp op->ks second) edits)))

(defn summarize [edits]
  (->> edits
       (group-by (comp grouping-path first))
       (mapv
        (fn [[path xs]]
          (assoc (get-stats xs) :path path)))))
