(ns dataspex.render-client
  (:require [dataspex.actions :as actions]
            [dataspex.hiccup :as hiccup]
            [dataspex.icons :as icons]
            [dataspex.ui :as ui]
            [replicant.dom :as r]))

::icons/keep
::ui/keep

(defprotocol HostChannel
  (initialize! [host-channel render-f])
  (process-actions [host-channel node actions]))

(defn prefers-dark-mode? []
  (.-matches (js/window.matchMedia "(prefers-color-scheme: dark)")))

(defn get-preferred-theme []
  (if (prefers-dark-mode?)
    :dark
    :light))

(defn execute-effects [effects]
  (doseq [[effect & args] effects]
    (case effect
      :effect/copy
      (actions/copy-to-clipboard (first args)))

    (prn effect args)))

(defn set-dispatch! [channels]
  (r/set-dispatch!
   (fn [{:replicant/keys [^js dom-event ^js node]} actions]
     (.preventDefault dom-event)
     (.stopPropagation dom-event)
     (let [channel (keyword (.getAttribute (.closest node "[data-channel]") "data-channel"))]
       (doseq [action actions]
         (apply prn channel action))
       (-> (process-actions (get channels channel) node actions)
           (.then execute-effects))))))

(defn ensure-element [^js root id]
  (if-let [el (js/document.getElementById id)]
    el
    (let [el (js/document.createElement "div")]
      (set! (.-id el) id)
      (.setAttribute el "data-channel" id)
      (.appendChild root el)
      el)))

(defn render-splash []
  [::ui/card-list#dataspex.code
   [ui/card-body
    [:p
     "Well, it ain't much to look at - yet. "
     "You can fix that by telling Dataspex to inspect something:"]
    (hiccup/render-source
     '(require [dataspex.core :as dataspex])
     {})
    (hiccup/render-source
     '(dataspex/inspect "App state" my-data)
     {})
    [:p
     "Dataspex can inspect pretty much anything you can throw at it: maps,
     vectors and lists, atoms, Datascript and Datomic databases, and anything
     that implements Clojure's Datafy. You can also implement Dataspex' own
     protocols to create custom data browsers. Have fun!"]]])

(defn mount-splash [root]
  (-> (ensure-element root "dataspex-splash")
      (r/render (render-splash))))

(defn render [^js el id hiccup]
  (when hiccup
    (when-let [splash (.querySelector el "#dataspex-splash")]
      (r/unmount splash)))
  (-> (ensure-element el (name id))
      (r/render hiccup))
  (when-not hiccup
    (when (->> (into [] (.-childNodes el))
               (filterv (comp #{1} #(.-nodeType %)))
               (every? #(empty? (.-innerHTML %))))
      (mount-splash el))))

(defn ^{:indent 1} start-render-client [^js root {:keys [channels]}]
  (let [theme (get-preferred-theme)]
    (.add (.-classList js/document.documentElement) (name theme))
    (mount-splash root)
    (set-dispatch! channels)
    (doseq [[id channel] channels]
      (initialize! channel #(render root id %))
      (->> [[:dataspex.actions/assoc-in [:dataspex/theme] theme]]
           (process-actions channel js/document.documentElement)))))
