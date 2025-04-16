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
