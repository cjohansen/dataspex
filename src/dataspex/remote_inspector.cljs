(ns dataspex.remote-inspector
  (:require [cljs.reader :as reader]
            [dataspex.actions :as actions]
            [dataspex.frontend :as frontend]))

(defn execute-effects [effects]
  (doseq [[effect & args] effects]
    (case effect
      :effect/copy
      (actions/copy-to-clipboard (first args)))

    (prn effect args)))

(frontend/set-dispatch!
 #(-> (js/fetch "/actions"
                (clj->js {:method "POST"
                          :headers {"Content-Type" "application/edn"}
                          :body (pr-str %)}))
      (.then (fn [res] (.text res)))
      (.then (fn [text]
               (execute-effects (reader/read-string text))))))

(frontend/init-element js/document.documentElement)

(defn ^:export main []
  (let [event-source (js/EventSource. "/renders")]
    (.addEventListener
     event-source "message"
     (fn [e]
       (println "Render")
       (frontend/render (reader/read-string (.-data e)))))

    (.addEventListener
     event-source "error"
     (fn [e]
       (js/console.error "EventSource error:" e)))))
