(ns dataspex.render-client
  (:require [clojure.walk :as walk]
            [dataspex.actions :as actions]
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

(defn get-form-data [^js form]
  (->> (into-array (.-elements form))
       (reduce
        (fn [res ^js el]
          (let [k (some-> el .-name not-empty keyword)]
            (cond-> res
              k (assoc k (.-value el)))))
        {})))

(defn interpolate [^js event actions]
  (walk/postwalk
   (fn [x]
     (case x
       :event.target/value (some-> event .-target .-value)
       :event/form-data (some-> event .-target get-form-data)
       x))
   actions))

(defn set-dispatch! [store handle-remotes-actions]
  (r/set-dispatch!
   (fn [{:replicant/keys [^js dom-event ^js node]} actions]
     (.preventDefault dom-event)
     (.stopPropagation dom-event)
     (let [channel (.getAttribute (.closest node "[data-channel]") "data-channel")
           actions (interpolate dom-event actions)]
       (doseq [action actions]
         (apply prn channel action))
       (if (= "dataspex-remotes" channel)
         (handle-remotes-actions actions)
         (-> (process-actions (get (:channels @store) channel) node actions)
             (.then execute-effects)))))))

(defn ensure-element [^js root channel-id & [{:keys [error?]}]]
  (let [id (str "el-" (hash channel-id))
        el-id (str id (when error? "-error"))]
    (if-let [el (js/document.getElementById el-id)]
      el
      (let [el (js/document.createElement "div")]
        (set! (.-id el) el-id)
        (if error?
          (.setAttribute el "data-error" "error")
          (.setAttribute el "data-channel" channel-id))
        (if error?
          (.appendChild root el)
          (if-let [anchor (.querySelector root "[data-error]")]
            (.insertAdjacentElement anchor "beforebegin" el)
            (.appendChild root el)))
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

(defn render [^js el id hiccup & [opt]]
  (when hiccup
    (when-let [splash (ensure-element el "dataspex-splash")]
      (r/unmount splash)))
  (-> (ensure-element el (name id) opt)
      (r/render hiccup))
  (when-not hiccup
    (when (->> (into [] (.-childNodes el))
               (filterv (comp #{1} #(.-nodeType %)))
               (every? #(empty? (.-innerHTML %))))
      (mount-splash el))))

(defn set-theme! [theme]
  (.setAttribute js/document.documentElement "data-theme" (name theme)))

(defn ^{:indent 2} add-channel [store id channel]
  (let [{:keys [root]} @store]
    (ensure-element root id)
    (swap! store assoc-in [:channels id] channel)
    (connect channel (fn [hiccup & [opt]]
                       (render root id hiccup opt)))))

(defn remove-channel [store id]
  (let [el ^js (ensure-element (:root @store) id)
        channel (get-in @store [:channels id])]
    (swap! store update :channels dissoc id)
    (when el
      (.removeChild (.-parentNode el) el))
    (when channel
      (disconnect channel))))

(defn init-el [root]
  (.add (.-classList root) "inspector")
  ;; This `into` indirection ensures the elements are created in the desired
  ;; order
  (into {} [[:panels-el (ensure-element root "dataspex-panels")]
            [:remotes-el (ensure-element root "dataspex-remotes")]]))

(defn ^{:indent 1} start-render-client [^js root {:keys [handle-remotes-actions
                                                         render-remotes]}]
  (let [{:keys [panels-el remotes-el]} (init-el root)
        store (atom {:root panels-el
                     :channels {}
                     :remotes {}})]
    (set-theme! (get-preferred-theme))
    (mount-splash panels-el)
    (set-dispatch! store (partial handle-remotes-actions store panels-el))
    (add-watch store ::render-remotes
     (fn [_ _ _ state]
       (r/render remotes-el (render-remotes state))))
    store))
