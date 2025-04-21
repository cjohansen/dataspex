(ns dataspex.remote-inspector
  (:require [cognitect.transit :as transit]
            [dataspex.actions :as actions]
            [dataspex.frontend :as frontend]))

(def reader (transit/reader :json))

(defn parse-transit [s]
  (transit/read reader s))

(def writer (transit/writer :json))

(defn generate-transit [data]
  (transit/write writer data))

(defn execute-effects [effects]
  (doseq [[effect & args] effects]
    (case effect
      :effect/copy
      (actions/copy-to-clipboard (first args)))

    (prn effect args)))

(defn post-actions [actions]
  (-> (js/fetch "/actions"
                (clj->js {:method "POST"
                          :headers {"Content-Type" "application/edn"}
                          :body (generate-transit actions)}))
      (.then (fn [res] (.text res)))
      (.then (fn [text]
               (execute-effects (parse-transit text))))))

(frontend/set-dispatch! post-actions)

(defn init-theme [theme]
  (post-actions [[:dataspex.actions/assoc-in [:dataspex/theme] theme]])
  (.add (.-classList js/document.documentElement) (name theme)))

(defn ^:export main []
  (init-theme (frontend/get-preferred-theme))
  (let [event-source (js/EventSource. "/renders")]
    (.addEventListener
     event-source "message"
     (fn [e]
       (println "Render")
       (frontend/render (parse-transit (.-data e)))))

    (.addEventListener
     event-source "error"
     (fn [e]
       (js/console.error "EventSource error:" e)))))
