(ns dataspex.render-client
  (:require [dataspex.actions :as actions]
            [dataspex.hiccup :as hiccup]
            [dataspex.icons :as icons]
            [dataspex.ui :as ui]
            [replicant.dom :as r]))

::icons/keep
::ui/keep

(defprotocol HostChannel
  (connect [host-channel render-f])
  (disconnect [host-channel])
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

(defn set-dispatch! [store]
  (r/set-dispatch!
   (fn [{:replicant/keys [^js dom-event ^js node]} actions]
     (.preventDefault dom-event)
     (.stopPropagation dom-event)
     (let [channel (.getAttribute (.closest node "[data-channel]") "data-channel")]
       (doseq [action actions]
         (apply prn channel action))
       (-> (process-actions (get (:channels @store) channel) node actions)
           (.then execute-effects))))))

(defn ensure-element [^js root channel-id]
  (let [id (str "el-" (hash channel-id))]
    (if-let [el (js/document.getElementById id)]
      el
      (let [el (js/document.createElement "div")]
        (set! (.-id el) id)
        (.setAttribute el "data-channel" channel-id)
        (.appendChild root el)
        el))))

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
    (when-let [splash (ensure-element el "dataspex-splash")]
      (r/unmount splash)))
  (-> (ensure-element el (name id))
      (r/render hiccup))
  (when-not hiccup
    (when (->> (into [] (.-childNodes el))
               (filterv (comp #{1} #(.-nodeType %)))
               (every? #(empty? (.-innerHTML %))))
      (mount-splash el))))

(defn set-theme! [theme]
  (.setAttribute js/document.documentElement "data-theme" (name theme)))

(defn ^{:indent 2} add-channel [store id channel]
  (let [{:keys [root theme]} @store]
    (ensure-element root id)
    (swap! store assoc-in [:channels id] channel)
    (connect channel #(render root id %))
    (->> [[:dataspex.actions/assoc-in [:dataspex/theme] theme]]
         (process-actions channel js/document.documentElement))))

(defn remove-channel [store id]
  (let [el ^js (ensure-element (:root @store) id)
        channel (get-in @store [:channels id])]
    (swap! store update :channels dissoc id)
    (when el
      (.removeChild (.-parentNode el) el))
    (when channel
      (disconnect channel))))

(defn ^{:indent 1} start-render-client [^js root]
  (let [theme (get-preferred-theme)
        store (atom {:root root
                     :theme theme
                     :channels {}
                     :remotes {}})]
    (set-theme! theme)
    (mount-splash root)
    (set-dispatch! store)
    store))
