(ns dataspex.server-channel
  (:require [dataspex.render-client :as rc]))

(defn connect-event-source [host render]
  (let [event-source (js/EventSource. (str host "/renders"))]
    (.addEventListener
     event-source "message"
     (fn [e]
       (println "Render")
       (render (rc/parse-string (.-data e)))))

    (.addEventListener
     event-source "error"
     (fn [e]
       (js/console.error "EventSource error:" e)))))

(defn post-actions [host actions]
  (-> (js/fetch (str host "/actions")
                (clj->js {:method "POST"
                          :headers {"Content-Type" "application/edn"}
                          :body (rc/generate-string actions)}))
      (.then (fn [res] (.text res)))
      (.then (fn [text] (rc/parse-string text)))))

(defrecord ServerChannel [host]
  rc/HostChannel
  (initialize! [_ render-f]
    (connect-event-source host render-f))

  (process-actions [_ actions]
    (post-actions host actions)))

(defn create-channel [& [host]]
  (ServerChannel. host))
