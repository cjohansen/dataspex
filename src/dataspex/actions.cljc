(ns dataspex.actions
  (:require [dataspex.data :as data]
            [dataspex.inspector :as inspector]
            [dataspex.protocols :as dp]
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

(defn copy-as-string [v]
  (if (satisfies? dp/ICopy v)
    (dp/copy-as-string v)
    (data/stringify v)))

(defn action->effects [state [action & args]]
  (case action
    ::assoc-in
    [(into [:effect/assoc-in] args)]

    ::copy
    (let [[label path] args]
      [[:effect/copy (-> (get-in state [label :val])
                         (data/nav-in path)
                         copy-as-string)]])

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

    ::navigate
    (let [[inspectee path] args
          target (data/nav-in (get-in state [inspectee :val]) path)]
      (cond-> [[:effect/assoc-in [inspectee :dataspex/path] path]]
        (not= :dataspex.activity/browse (get-in state [inspectee :dataspex/activity]))
        (conj [:effect/assoc-in [inspectee :dataspex/activity] :dataspex.activity/browse])

        (data/element? target)
        (concat (let [id (str "el" (hash target))]
                  [[:effect/inspect-in-devtools id]
                   [:effect/expose-for-inspection id target]]))))
    
    ::reset-ref-to-revision
    (let [[label rev] args
          revision (->> (get-in state [label :history])
                        (filterv (comp #{rev} :rev))
                        first)]
      [[:effect/reset-ref
        label
        (:val revision)]])

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

    :effect/expose-for-inspection
    (doseq [[id target] args]
      #?(:cljs (when (exists? js/window)
                 (set! js/window.__DATASPEX__ (or js/window.__DATASPEX__ #js {}))
                 (aset js/window.__DATASPEX__ id target))
         :clj [id target]))

    ;; This effect only runs in the browser extension,
    ;; see dataspex.render-client
    :effect/inspect-in-devtools
    nil

    :effect/inspect
    (doseq [[label current value opts] args]
      (->> (inspector/inspect-val current value opts)
           (swap! store assoc label)))

    :effect/reset-ref
    (doseq [[label current] args]
      (let [ref (get-in @store [label :ref])]
        (dp/unwatch ref :dataspex.inspector/inspect)
        (reset! ref current)
        (dp/watch ref :dataspex.inspector/inspect (inspector/watch-fn store label))))

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
