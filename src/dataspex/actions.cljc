(ns dataspex.actions
  (:require [dataspex.data :as data]
            [dataspex.inspector :as inspector]
            [dataspex.time :as time]))

(defn to-clipboard [#?(:cljs text :clj _)]
  #?(:cljs
     (let [text-area (js/document.createElement "textarea")]
       (set! (.-textContent text-area) text)
       (js/document.body.appendChild text-area)
       (.select text-area)
       (js/document.execCommand "copy")
       (.blur text-area)
       (js/document.body.removeChild text-area))))

(defn handle-action [state [action & args]]
  (case action
    ::assoc-in
    [(into [:effect/assoc-in] args)]

    ::copy
    (let [[label path] args]
      [[:effect/copy (-> (get-in state [label :val])
                         (data/nav-in path)
                         data/stringify)]])

    ::inspect-revision
    (let [[label rev] args
          revision (->> (get-in state [label :history])
                        (filterv (comp #{rev} :rev))
                        first)]
      [[:effect/inspect
        (str label "@" (time/hh:mm:ss (:created-at revision)))
        nil
        (:val revision)
        {:auditable? false}]])

    ::uninspect
    [[:effect/uninspect (first args)]]))

(defn assoc-in* [m kvs]
  (reduce (fn [m [path v]]
            (assoc-in m path v)) m kvs))

(defn process-effects [store effects]
  (case (ffirst effects)
    :effect/assoc-in
    (swap! store assoc-in* (mapv #(drop 1 %) effects))

    :effect/copy
    (doseq [[_ text] effects]
      (to-clipboard text))

    :effect/inspect
    (doseq [[_ label current value opts] effects]
      (->> (inspector/inspect-val current value opts)
           (swap! store assoc label)))

    :effect/uninspect
    (doseq [[_ label] effects]
      (inspector/uninspect store label))

    (println "Unknown effect" effects)))

(defn ^{:indent 1} handle-actions [store actions]
  (let [state @store]
    (->> (mapcat #(handle-action state %) actions)
         (group-by first)
         (run! #(process-effects store (second %))))))
