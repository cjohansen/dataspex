(ns dataspex.panel
  (:require [dataspex.actions :as-alias actions]
            [dataspex.ui :as-alias ui]
            [dataspex.icons :as-alias icons]))

(defn render? [opt]
  (get opt :dataspex/render? true))

(defn render-title-bar [{:dataspex/keys [path inspectee] :as opt}]
  [::ui/toolbar
   [::ui/tabs
    [::ui/tab inspectee]
    (cond-> [::ui/tab]
      path (conj {::ui/selected? true})
      :then (conj "Browse"))]
   [::ui/button-bar
    (if (render? opt)
      [::ui/button {::ui/title "Minimize"
                    ::ui/actions [[::actions/assoc-in [inspectee :dataspex/render?] false]]}
       [::icons/arrows-in-simple]]
      [::ui/button {::ui/title "Maximize"
                    ::ui/actions [[::actions/assoc-in [inspectee :dataspex/render?] true]]}
       [::icons/arrows-out-simple]])
    [::ui/button {::ui/title "Close"
                  ::ui/actions [[::actions/uninspect inspectee]]}
     [::icons/x]]]])
