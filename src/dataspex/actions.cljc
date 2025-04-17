(ns dataspex.actions
  (:require [dataspex.inspector :as inspector]
            [dataspex.data :as data]))

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

    :effect/uninspect
    (doseq [[_ label] effects]
      (inspector/uninspect store label))

    (println "Unknown effect" effects)))

(defn ^{:indent 1} handle-actions [store actions]
  (let [state @store]
    (->> (mapcat #(handle-action state %) actions)
         (group-by first)
         (run! #(process-effects store (second %))))))
