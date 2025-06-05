(ns dataspex.remotes-panel
  (:require [clojure.string :as str]
            [dataspex.actions :as actions]
            [dataspex.codec :as codec]
            [dataspex.icons :as-alias icons]
            [dataspex.render-client :as rc]
            [dataspex.server-client :as server-client]
            [dataspex.ui :as-alias ui]))

(defn host->id [host]
  (-> host
      (str/replace #"^.*://" "")
      (str/replace #":" "-")
      (str/replace #"/" "-")))

(defn add-channel [client host & [{:keys [on-message]}]]
  (let [id (host->id host)]
    (swap! client update-in [:remotes id] merge
           {:host (second (re-find #"(.*://[^/]+)" host))
            :url host})
    (->> {:on-connection-status-changed
          (fn [{:keys [connected?]}]
            (swap! client assoc-in [:remotes id :connected?] connected?))
          :on-message on-message}
         (server-client/create-channel host)
         (rc/add-channel client id))))

(defn remove-channel [client host]
  (rc/remove-channel client (host->id host)))

(defn update-persistent-remotes [state new-remotes]
  (when (and (:dataspex/inspected-host state)
             (not= new-remotes (:dataspex/persistent-remotes state)))
    [[:effect/assoc-in [:dataspex/persistent-remotes] new-remotes]
     [:effect/persist-remotes (:dataspex/inspected-host state) new-remotes]]))

(defn fully-qualify-hosts [host-str]
  (let [host (-> (if (re-find #"^https?://" host-str)
                   host-str
                   (str "http://" host-str))
                 (str/replace #"/$" ""))]
    (if (re-find #"^.*://[^/]+/(jvm|relay)" host)
      [host]
      [(str host "/jvm")
       (str host "/relay")])))

(defn action->effects [state action]
  (case (first action)
    ::actions/reconnect-channel
    (let [persistent-remotes (set (:dataspex/persistent-remotes state))
          host (get-in state [:remotes (second action) :host])
          candidates (->> (:remotes state)
                          (filter #(= host (:host (second %)))))]
      (concat (for [id (map first candidates)]
                [:effect/reconnect-channel id])
              (->> (map (comp :url second) candidates)
                   (into persistent-remotes)
                   (update-persistent-remotes state))))

    ::actions/disconnect-channel
    (let [persistent-remotes (set (:dataspex/persistent-remotes state))
          host (get-in state [:remotes (second action) :host])
          candidates (->> (:remotes state)
                          (filter #(= host (:host (second %)))))]
      (concat (for [id (map first candidates)]
                [:effect/disconnect-channel id])
              (->> (remove (set (map (comp :url second) candidates)) persistent-remotes)
                   (update-persistent-remotes state))))

    ::actions/connect-channel
    (let [hosts (fully-qualify-hosts (:host (second action)))
          persistent-remotes (:dataspex/persistent-remotes state)]
      (cond-> (for [host hosts]
                [:effect/connect-channel host])
        (:dataspex/inspected-host state)
        (into (update-persistent-remotes state (into persistent-remotes hosts)))))

    (actions/action->effects state action)))

(defn execute-batched-effect! [store ^js root {:keys [effect args] :as batch}]
  (prn effect args)
  (case effect
    :effect/reconnect-channel
    (doseq [[id] args]
      (rc/connect (get-in @store [:channels id]) #(rc/render root id %)))

    :effect/disconnect-channel
    (doseq [[id] args]
      (rc/disconnect (get-in @store [:channels id]))
      (rc/render root id nil))

    :effect/connect-channel
    (doseq [[host] args]
      (add-channel store host))

    :effect/persist-remotes
    (doseq [[inspected-host new-remotes] args]
      (.set (.-local js/chrome.storage)
            (clj->js {inspected-host (codec/generate-string new-remotes)})))

    (actions/execute-batched-effect! store batch)))

(defn handle-actions [store root actions]
  (let [state @store]
    (->> actions
         (mapcat #(action->effects state %))
         actions/batch-effects
         (run! #(execute-batched-effect! store root %)))))

(defn render-panel [{:keys [remotes]}]
  [:div.flex.space-between.p-2
   (into [:div.flex.gap-8]
         (when-not (:add? remotes)
           (for [[host [{:keys [id connected?]}]]
                 (->> remotes
                      (filter (comp string? key))
                      (map #(assoc (second %) :id (first %)))
                      (group-by :host))]
             [:span.subtle.flex.gap-2.discrete-button
              (if connected?
                [::ui/button {::ui/actions [[::actions/disconnect-channel id]]}
                 [::icons/wifi-high {:class "hover_hide"
                                     :data-color "success"}]
                 [::icons/x {:class "hover_show"}]
                 host]
                [:span.subtle.flex.gap-2.discrete-button
                 [::ui/button {::ui/actions [[::actions/reconnect-channel id]]}
                  [::icons/wifi-x {:class "hover_hide"
                                   :data-color "error"}]
                  [::icons/arrow-counter-clockwise {:class "hover_show"}]
                  host]])])))
   (if (:add? remotes)
     [:form.flex.gap-2 {:on {:submit [[::actions/connect-channel :event/form-data]
                                      [::actions/assoc-in [:remotes :add?] false]]}}
      [::ui/input {:name "host"
                   :type "text"
                   ::ui/autofocus? true}]
      [::ui/button {:type "submit"}
       [::icons/plus-circle {:data-color "success"}]]
      [::ui/button {::ui/actions [[::actions/assoc-in [:remotes :add?] false]]}
       [::icons/x]]]
     [::ui/button {::ui/actions [[::actions/assoc-in [:remotes :add?] true]]}
      [::icons/plus-circle]])])
