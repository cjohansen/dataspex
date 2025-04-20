(ns dataspex.actions
  (:require [dataspex.data :as data]
            [dataspex.inspector :as inspector]
            [dataspex.time :as time]))

(defn copy-to-clipboard [#?(:cljs text :clj _)]
  #?(:cljs
     (let [text-area (js/document.createElement "textarea")]
       (set! (.-textContent text-area) text)
       (js/document.body.appendChild text-area)
       (.select text-area)
       (js/document.execCommand "copy")
       (.blur text-area)
       (js/document.body.removeChild text-area))))

(defn action->effects [state [action & args]]
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

(defn execute-batched-effect! [store {:keys [effect args]}]
  (case effect
    :effect/assoc-in
    (swap! store assoc-in* args)

    :effect/copy
    (doseq [[text] args]
      (copy-to-clipboard text))

    :effect/inspect
    (doseq [[label current value opts] args]
      (->> (inspector/inspect-val current value opts)
           (swap! store assoc label)))

    :effect/uninspect
    (doseq [[label] args]
      (inspector/uninspect store label))

    (println "Unknown effect" effect args)))

(defn batch-effects [effects]
  (->> (group-by first effects)
       (mapv
        (fn [[effect xs]]
          {:effect effect
           :args (mapv #(drop 1 %) xs)}))))

(defn ^:export execute-sequentially [store effects]
  (doseq [[effect & args] effects]
    (execute-batched-effect! store {:effect effect :args [args]})))

(defn ^:export execute-batched! [store effects]
  (->> (batch-effects effects)
       (run! #(execute-batched-effect! store %))))

(defn plan [state actions]
  (mapcat #(action->effects state %) actions))

(defn ^{:indent 1} act! [store actions]
  (->> (plan @store actions)
       (execute-batched! store)))
