(ns dataspex.actions)

(defn handle-action [_ [action & args]]
  (case action
    ::assoc-in
    [(into [:effect/assoc-in] args)]

    ::copy
    []

    ::uninspect
    []))

(defn assoc-in* [m kvs]
  (reduce (fn [m [path v]]
            (assoc-in m path v)) m kvs))

(defn process-effects [store effects]
  (case (ffirst effects)
    :effect/assoc-in
    (swap! store assoc-in* (mapv #(drop 1 %) effects))

    (println "Unknown effect" effects)))

(defn handle-actions [store actions]
  (let [state @store]
    (->> (mapcat #(handle-action state %) actions)
         (group-by first)
         (run! #(process-effects store (second %))))))
