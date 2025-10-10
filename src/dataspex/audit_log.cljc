(ns dataspex.audit-log
  (:require [dataspex.actions :as-alias actions]
            [dataspex.data :as data]
            [dataspex.diff :as diff]
            [dataspex.hiccup :as hiccup]
            [dataspex.icons :as-alias icons]
            [dataspex.time :as time]
            [dataspex.ui :as-alias ui]
            [dataspex.views :as views]
            [dataspex.protocols :as dp]))

(def diff-op->el
  {:insertions ::ui/success
   :deletions ::ui/error})

(def diff-op->label
  {:insertions "insertion"
   :deletions "deletion"})

(defn render-inline [v & [opt]]
  (hiccup/render-inline v (assoc opt :dataspex/summarize-above-w -1)))

(defn render-elaborate-diff-summary [diff]
  (let [stats (diff/get-stats diff)
        paths (set (mapv first diff))]
    (-> [:div.grow]
        (into
         (loop [[[k n] & ks] (->> [:insertions :deletions :replacements]
                                  (mapv (juxt identity stats))
                                  (remove (comp nil? second)))
                res []]
           (if (nil? k)
             res
             (recur
              ks
              (let [label (hiccup/inflect n (diff-op->label k))]
                (if-let [el (diff-op->el k)]
                  (into res [[el (str n)]
                             (str " " label (when ks ", "))])
                  (str n " " label (when ks ", "))))))))
        (into
         (cond
           (= 1 (count paths))
           [" in " (render-inline
                    (if (= 1 (count (first paths)))
                      (ffirst paths)
                      (first paths)))]

           :else
           [(str " in " (count paths) " keys")])))))

(defn ^{:indent 1} render-diff-summary [x diff]
  (if (satisfies? dp/IRenderDiffSummary x)
    (dp/render-diff-summary x diff)
    (render-elaborate-diff-summary diff)))

(defn render-diffs [diff opt]
  (->> (group-by first diff)
       (sort-by first)
       (mapv
        (fn [[path edits]]
          (into [:article.diff
                 [::ui/source (render-inline path (assoc opt :dataspex/hiccup? false))]]
                (mapv
                 (fn [[_ op v]]
                   [::ui/source {::ui/prefix (name op)
                                 :data-color (if (= :- op) "error" "success")}
                    (render-inline v opt)])
                 edits))))))

(defn ^{:indent 1} render-diff-details [x diff opt]
  (if (satisfies? dp/IRenderDiff x)
    (dp/render-diff x diff)
    (render-diffs diff opt)))

(defn render-custom-summary [summary diff]
  [:div.grow.flex.space-between
   (render-inline summary)
   (let [stats (diff/get-stats diff)]
     (some->> [(when-let [n (:insertions stats)]
                 [::ui/success (str "+" n)])
               (when (and (:insertions stats) (:deletions stats))
                 " ")
               (when-let [n (:deletions stats)]
                 [::ui/error (str "-" n)])]
              (remove nil?)
              seq
              (into [:div.tag])))])

(defn render-browse-rev-button [{:keys [rev current?]} opt]
  [::ui/button
   (if current?
     {::ui/selected? true
      ::ui/title "Current version"}
     {::ui/actions [[::actions/inspect-revision (:dataspex/inspectee opt) rev]]
      ::ui/title "Browse this version"})
   [::icons/browser]])

(defn render-render-rev-button [{:keys [rev]} opt]
  [::ui/button
   {::ui/actions [[::actions/reset-ref-to-revision (:dataspex/inspectee opt) rev]],
    ::ui/title "Render this version"}
   [::icons/eye]])

(defn render-revision [{:keys [created-at diff rev current? ref-resettable? dataspex.audit/summary] :as revision} opt]
  (let [fold-path [::audit-log :rev rev]
        folded? (get-in opt [:dataspex/folding fold-path :folded?] true)
        foldable? (not-empty diff)]
    (cond-> [::ui/card
             [::ui/card-header
              (cond-> {}
                foldable? (assoc ::ui/actions [(views/update-folding (dissoc opt :dataspex/path) fold-path {:folded? (not folded?)})]))
              [::ui/timestamp
               {:data-folded (if foldable?
                               (str folded?)
                               "" ;; Keeps the element aligned with the foldable ones
                               )}
               (time/hh:mm:ss created-at)]
              (cond
                summary (render-custom-summary summary diff)
                diff (render-diff-summary (:val revision) diff)
                :else [:div.grow])
              (when folded?
                [:div.buttons
                 (render-browse-rev-button revision opt)
                 (when ref-resettable? (render-render-rev-button revision opt))])]]
      (not folded?)
      (conj (cond-> [::ui/card-body]
              :then (into (render-diff-details (:val revision) (:diff revision) opt))
              (not current?)
              (conj [:div (-> (render-browse-rev-button revision opt)
                              (conj "Browse this version"))]))))))

(defn render-change-tracking-instructions [{:dataspex/keys [inspectee] :keys [ref]}]
  [::ui/card
   [::ui/card-body
    [:p (str inspectee " isn’t currently set up to track changes. "
             "To turn it on, call inspect with the following options:")]
    [::ui/source
     [::ui/list
      [::ui/symbol 'dataspex/inspect]
      [::ui/string inspectee]
      [::ui/symbol (if ref 'ref 'data)]
      [::ui/map
       [::ui/map-entry
        [::ui/keyword :track-changes?]
        [::ui/boolean true]]
       [::ui/map-entry
        [::ui/keyword :history-limit]
        [::ui/number 25]]]]]]])

(defn render-log [inspectee-state opt]
  (into [::ui/card-list.code]
        (if (:history inspectee-state)
          (let [overflow (- (:rev inspectee-state) (count (:history inspectee-state)))]
            (cond-> (mapv
                     (fn [revision]
                       (-> revision
                           (assoc :current? (= (:rev revision) (:rev inspectee-state)))
                           (assoc :ref-resettable? (data/resettable? (:ref inspectee-state)))
                           (render-revision opt)))
                     (:history inspectee-state))
              (< 0 overflow)
              (conj [::ui/card
                     [::ui/card-body
                      [:p
                       (str overflow " older versions have been discarded. Change ")
                       [::ui/keyword :history-limit]
                       " to control how history is truncated."]]])))
          [(render-change-tracking-instructions inspectee-state)])))
