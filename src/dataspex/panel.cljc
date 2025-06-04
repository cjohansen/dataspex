(ns dataspex.panel
  (:require [clojure.string :as str]
            [dataspex.actions :as-alias actions]
            [dataspex.audit-log :as audit-log]
            [dataspex.data :as data]
            [dataspex.hiccup :as hiccup]
            [dataspex.icons :as-alias icons]
            [dataspex.ui :as-alias ui]
            [dataspex.views :as views]))

(def browse :dataspex.activity/browse)
(def audit :dataspex.activity/audit)

(defn render? [opt]
  (get opt :dataspex/render? true))

(defn render-tab [{:dataspex/keys [activity inspectee]} tab-activity]
  [::ui/tab
   (if (= (or activity browse) tab-activity)
     {::ui/selected? true}
     {::ui/actions [[::actions/assoc-in [inspectee :dataspex/activity] tab-activity]]})
   (str/capitalize (name tab-activity))])

(def other-theme
  {:light :dark
   :dark :light})

(defn get-theme [opt]
  (or (:dataspex/theme opt) :dark))

(defn render-title-bar [{:keys [history]} {:dataspex/keys [inspectee host-str] :as opt}]
  (let [rendering? (render? opt)
        theme (get-theme opt)]
    [::ui/toolbar
     (cond-> [::ui/tabs]
       rendering?
       (conj (render-tab opt browse))

       (and rendering? (get opt :dataspex/auditable? true) (< 1 (count (or history []))))
       (conj (render-tab opt audit)))
     (cond-> [:h2 [:strong inspectee]]
       host-str (conj [:span.subtle.ml-4 host-str]))
     (cond-> [::ui/button-bar]
       rendering? (conj [::ui/button
                         {::ui/title (str "Switch to " (name (other-theme theme)) " mode")
                          ::ui/actions [[::actions/assoc-in [inspectee :dataspex/theme]
                                         (other-theme theme)]]}
                         (if (= theme :light)
                           [::icons/moon]
                           [::icons/sun])])

       :then
       (into [(if rendering?
                [::ui/button {::ui/title "Minimize"
                              ::ui/actions [[::actions/assoc-in [inspectee :dataspex/render?] false]]}
                 [::icons/arrows-in-simple]]
                [::ui/button {::ui/title "Maximize"
                              ::ui/actions [[::actions/assoc-in [inspectee :dataspex/render?] true]]}
                 [::icons/arrows-out-simple]])
              [::ui/button {::ui/title "Close"
                            ::ui/actions [[::actions/uninspect inspectee]]}
               [::icons/x]]]))]))

(def views
  [{:view views/dictionary
    :label "in data browser"
    :icon [::icons/browser]}
   {:view views/source
    :label "raw data"}
   {:view views/table
    :label "as table"
    :icon [::icons/table]}])

(defn render-view-menu [v opt]
  (let [source-icon (cond
                      (vector? v) [::icons/brackets-square]
                      (map? v) [::icons/brackets-curly]
                      (seq? v) [::icons/brackets-round]
                      :else [::icons/brackets-curly])]
    (into
     [::ui/button-bar]
     (mapv
      (fn [{:keys [view label icon]}]
        (if (= view (:dataspex/view opt))
          [::ui/button
           {::ui/title (str "Viewing " label)
            ::ui/selected? true}
           (or icon source-icon)]
          [::ui/button
           (if (data/supports-view? v view opt)
             {::ui/title (str "View " label)
              ::ui/actions [[::actions/assoc-in [(:dataspex/inspectee opt) :dataspex/view (:dataspex/path opt)] view]
                            [::actions/assoc-in [(:dataspex/inspectee opt) :dataspex/default-view] view]]}
             {::ui/title (str "The data doesn't support the " (name view) " view")})
           (or icon source-icon)]))
      views))))

(defn render-path-k [k opt]
  (-> (data/inspect k opt)
      (hiccup/render-inline opt)))

(defn render-path [path opt]
  (let [n (count path)
        opt (assoc opt
                   :dataspex/view views/inline
                   :dataspex/summarize-above-w 40)
        path-elements (->> (butlast path)
                           (reduce
                            (fn [{:keys [curr res]} e]
                              (let [curr (conj curr e)]
                                {:curr curr
                                 :res (conj res
                                            [::ui/crumb
                                             {::ui/actions (views/navigate-to opt curr)}
                                             (render-path-k e opt)])}))
                            {:curr []
                             :res []})
                           :res)]
    (cond-> [::ui/path
             [::ui/crumb
              (cond-> {}
                (< 0 n)
                (assoc ::ui/actions (views/navigate-to opt [])))
              "."]]

      (< 4 n)
      (into
       [(first path-elements)
        [::ui/crumb [::ui/code "..."]]
        (take-last 2 path-elements)])

      (< 1 n 5)
      (into path-elements)

      (< 0 n)
      (conj [::ui/crumb (render-path-k (last path) opt)]))))

(defn render-pagination-bar [{:keys [page-size offset n]} opt]
  (when (and n (< page-size n))
    (let [prev-n (- offset page-size)
          next-n (+ offset page-size)
          max-n (if (< views/max-count n) (str views/max-count "+") n)]
      [::ui/navbar.center
       (cond-> [::ui/button]
         (<= 0 prev-n) (conj {::ui/actions [(views/offset-pagination opt prev-n)]})
         :then (conj [::icons/caret-left]))
       [:span.code.text-smaller.subtle
        (str offset "-" (min n (dec next-n)) " of " max-n "")]
       (cond-> [::ui/button]
         (< next-n n) (conj {::ui/actions [(views/offset-pagination opt next-n)]})
         :then (conj [::icons/caret-right]))])))

(defn render-data [x opt]
  (when (render? opt)
    (case (:dataspex/view opt)
      :dataspex.views/inline
      (hiccup/render-inline x opt)

      :dataspex.views/table
      (hiccup/render-table x opt)

      :dataspex.views/source
      (hiccup/render-source x opt)

      (hiccup/render-dictionary x opt))))

(defn possibly-scroll [hiccup opt]
  (cond->> hiccup
    (:dataspex/max-height opt)
    (conj [:div {:style {:max-height (:dataspex/max-height opt)
                         :overflow-y "scroll"}}])))

(defn render-panel [state label]
  (let [{:keys [data opt]} (views/get-render-data state label)
        rendering? (render? opt)]
    (into
     [:div.panel (cond-> {:data-theme (name (get-theme opt))}
                   (not rendering?) (assoc :data-folded "folded"))
      (render-title-bar (get state label) opt)]
     (when rendering?
       (if (= audit (:dataspex/activity opt))
         [(-> (get state label)
              (audit-log/render-log opt)
              (possibly-scroll opt))]
         (let [data-view (render-data data opt)
               pagination (render-pagination-bar (:dataspex/pagination (meta data-view)) opt)]
           [(when (render? opt)
              [::ui/navbar
               (render-path (:dataspex/path opt) opt)
               (render-view-menu data opt)])
            pagination
            [:main.scroll-x
             (possibly-scroll data-view opt)]
            pagination]))))))

(defn render-inspector [state]
  (some->> (keys state)
           (filterv string?)
           sort
           (mapv #(render-panel state %))
           not-empty
           (into [:div])))
