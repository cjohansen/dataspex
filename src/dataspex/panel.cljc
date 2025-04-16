(ns dataspex.panel
  (:require [dataspex.actions :as-alias actions]
            [dataspex.data :as data]
            [dataspex.hiccup :as hiccup]
            [dataspex.icons :as-alias icons]
            [dataspex.ui :as-alias ui]
            [dataspex.views :as views]))

(defn render? [opt]
  (get opt :dataspex/render? true))

(defn render-title-bar [{:dataspex/keys [path inspectee] :as opt}]
  (let [rendering? (render? opt)]
    [::ui/toolbar
     (cond-> [::ui/tabs [::ui/tab inspectee]]
       rendering?
       (conj (cond-> [::ui/tab]
               path (conj {::ui/selected? true})
               :then (conj "Browse"))))
     [::ui/button-bar
      (if rendering?
        [::ui/button {::ui/title "Minimize"
                      ::ui/actions [[::actions/assoc-in [inspectee :dataspex/render?] false]]}
         [::icons/arrows-in-simple]]
        [::ui/button {::ui/title "Maximize"
                      ::ui/actions [[::actions/assoc-in [inspectee :dataspex/render?] true]]}
         [::icons/arrows-out-simple]])
      [::ui/button {::ui/title "Close"
                    ::ui/actions [[::actions/uninspect inspectee]]}
       [::icons/x]]]]))

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
                      :else [::icons/brackets-curly])
        current (views/get-current-view v opt)]
    (into
     [::ui/button-bar]
     (mapv
      (fn [{:keys [view label icon]}]
        (if (= view current)
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

(defn render-path [path opt]
  (let [n (count path)]
    (cond-> [::ui/path
             [::ui/crumb
              (cond-> {}
                (< 0 n)
                (assoc ::ui/actions [(views/navigate-to opt [])]))
              "."]]

      (< 1 n)
      (into
       (->> (butlast path)
            (reduce
             (fn [{:keys [curr res]} e]
               (let [curr (conj curr e)]
                 {:curr curr
                  :res (conj res
                             [::ui/crumb
                              {::ui/actions [(views/navigate-to opt curr)]}
                              (hiccup/render-inline e opt)])}))
             {:curr []
              :res []})
            :res))

      (< 0 n)
      (conj [::ui/crumb
             (hiccup/render-inline (last path) opt)]))))

(defn render-pagination-bar [v opt]
  (when (coll? v)
    (let [{:keys [page-size offset]} (views/get-pagination opt)
          n (bounded-count (+ offset 1001) v)]
      (when (< page-size n)
        (let [prev-n (- offset page-size)
              next-n (+ offset page-size)]
          [::ui/navbar.center
           (cond-> [::ui/button]
             (<= 0 prev-n) (conj {::ui/actions [(views/offset-pagination opt prev-n)]})
             :then (conj [::icons/caret-left]))
           [:span.code.text-smaller.subtle
            (str offset "-" (dec next-n) " of " (if (< 1000 n) "1000+" n) "")]
           (cond-> [::ui/button]
             (< next-n n) (conj {::ui/actions [(views/offset-pagination opt next-n)]})
             :then (conj [::icons/caret-right]))])))))

(defn render-data [x opt]
  (when (render? opt)
    (case (views/get-current-view x opt)
      :dataspex.views/inline
      (hiccup/render-inline x opt)

      :dataspex.views/table
      (hiccup/render-table x opt)

      :dataspex.views/source
      (hiccup/render-source x opt)

      (hiccup/render-dictionary x opt))))

(defn render-panel [state label x]
  (let [opt (views/get-view-options state label)
        data (-> (data/nav-in x (:dataspex/path opt))
                 (data/inspect opt))
        pagination (render-pagination-bar data opt)]
    [:div.panel
     (render-title-bar opt)
     [:div
      (when (render? opt)
        [::ui/navbar
         (render-path (:dataspex/path opt) opt)
         (render-view-menu data opt)])
      pagination
      (render-data data opt)
      pagination]]))
