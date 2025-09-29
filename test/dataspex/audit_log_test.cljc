(ns dataspex.audit-log-test
  (:require [clojure.test :refer [deftest is testing]]
            [dataspex.actions :as-alias actions]
            [dataspex.audit-log :as audit-log]
            [dataspex.helper :as h]
            [dataspex.icons :as-alias icons]
            [dataspex.time :as time]
            [dataspex.ui :as-alias ui]
            [lookup.core :as lookup])
  #?(:clj (:import [java.time ZoneId])))

(deftest render-revision
  (testing "Renders time of change and diff summary"
    (is (= (-> (audit-log/render-revision
                {:created-at #inst "2025-04-16T16:21:14.000-00:00"
                 :rev 2
                 :val {:movie/title "Interstellar"
                       :movie/year 2014}
                 :diff [[[:movie/director] :-]
                        [[:movie/year] :+]
                        [[:movie/title] :+ "Interstellar"]]
                 :ref-is-atom? true}
                {:dataspex/inspectee "Store"
                 :dataspex/path []})
               h/strip-clock-times)
           [::ui/card
            [::ui/card-header
             {::ui/actions
              [[::actions/assoc-in
                ["Store" :dataspex/folding [:dataspex.audit-log/audit-log :rev 2]]
                {:folded? false}]]}
             [::ui/timestamp {:data-folded "true"} "HH:mm:ss"]
             [:div.grow
              [::ui/success "2"] " insertions, "
              [::ui/error "1"] " deletion"
              " in 3 keys"]
             [:div.buttons
              [::ui/button
               {:dataspex.ui/title "Browse this version"
                ::ui/actions [[::actions/inspect-revision "Store" 2]]}
               [::icons/browser]]
              [::ui/button
               {::ui/actions [[::actions/reset-ref-to-revision "Store" 2]],
                ::ui/title "Render this version"}
               [::icons/sun]]]]])))

  (testing "Renders more compact diff summary with custom audit log summary"
    (is (= (->> (with-redefs [time/get-default-timezone
                              (constantly #?(:clj (ZoneId/of "Europe/Oslo") :cljs nil))]
                  (audit-log/render-revision
                   {:created-at #inst "2025-04-16T12:46:19"
                    :rev 2
                    :diff [[[:movie/director] :-]
                           [[:movie/year] :+]
                           [[:movie/title] :+ "Interstellar"]]
                    :dataspex.audit/summary [:actions :make-it-go-boom]}
                   {:dataspex/inspectee "Store"
                    :dataspex/path []}))
                (lookup/select-one ::ui/card-header)
                lookup/children
                second
                h/strip-attrs)
           [:div
            [::ui/vector
             [::ui/keyword :actions]
             [::ui/keyword :make-it-go-boom]]
            [:div
             [::ui/success "+2"] " "
             [::ui/error "-1"]]])))

  (testing "Does not render diff when there is none"
    (is (= (->> (audit-log/render-revision
                 {:created-at #inst "2025-04-16T16:21:14.000-00:00"
                  :rev 1
                  :val {:movie/title "Interstellar"
                        :movie/year 2014}}
                 {:dataspex/inspectee "Store"
                  :dataspex/path []})
                (lookup/select [::ui/card-header :div.grow])
                lookup/text)
           "")))

  (testing "Does not render compact diff when there is none"
    (is (empty? (->> (audit-log/render-revision
                      {:created-at #inst "2025-04-16T16:21:14.000-00:00"
                       :rev 1
                       :val {:movie/title "Interstellar"
                             :movie/year 2014}
                       :dataspex.audit/summary [:actions :make-it-go-boom]}
                      {:dataspex/inspectee "Store"
                       :dataspex/path []})
                     (lookup/select [::ui/card-header :div.tag])))))

  (testing "Can't expand card when there's no diff"
    (is (empty?
         (->> (audit-log/render-revision
               {:created-at #inst "2025-04-16T16:21:14.000-00:00"
                :rev 1
                :val {:movie/title "Interstellar"
                      :movie/year 2014}}
               {:dataspex/inspectee "Store"
                :dataspex/path []})
              (lookup/select-one ::ui/card-header)
              lookup/attrs))))

  (testing "Renders diff for only one short path"
    (is (= (audit-log/render-diff-summary {}
             [[[:movie/director] :-]])
           [:div.grow [::ui/error "1"] " deletion" " in " [::ui/keyword :movie/director]])))

  (testing "Renders diff for one longer path"
    (is (= (->> [[[:movies :movie/director] :-]]
                (audit-log/render-diff-summary {})
                h/strip-attrs)
           [:div.grow
            [::ui/error "1"] " deletion" " in "
            [::ui/vector
             [::ui/keyword :movies]
             [::ui/keyword :movie/director]]]))))

(defn render-expanded [& [extra]]
  (with-redefs [time/get-default-timezone
                (constantly #?(:clj (ZoneId/of "Europe/Oslo") :cljs nil))]
    (audit-log/render-revision
     (merge {:created-at #inst "2025-04-16T16:21:14.000-00:00"
             :rev 2
             :val {:movie/title "Interstellar"
                   :movie/year 2014}
             :diff [[[:movie/year] :+ 2014]
                    [[:movie/director] :- "Christopher Nolan"]
                    [[:movie/title] :+ "Interstellar"]]}
            extra)
     {:dataspex/inspectee "Store"
      :dataspex/path [:stuff]
      :dataspex/folding {[:dataspex.audit-log/audit-log :rev 2]
                         {:folded? false}}})))

(deftest render-expanded-revision
  (testing "Displays card as unfolded"
    (is (= (->> (render-expanded)
                (lookup/select-one [::ui/card-header ::ui/timestamp])
                lookup/attrs
                :data-folded)
           "false")))

  (testing "Collapses card when clicking the header"
    (is (= (->> (render-expanded)
                (lookup/select-one ::ui/card-header)
                lookup/attrs
                ::ui/actions)
           [[:dataspex.actions/assoc-in
             ["Store" :dataspex/folding [:dataspex.audit-log/audit-log :rev 2]]
             {:folded? true}]])))

  (testing "When a revision is expanded removes the buttons from the header"
    (is (nil? (->> (render-expanded)
                   (lookup/select-one [::ui/card-header :div.buttons])))))

  (testing "Renders diff in card body"
    (is (= (->> (render-expanded)
                (lookup/select-one [::ui/card-body :article.diff])
                lookup/children
                (h/strip-attrs #{:dataspex.ui/actions}))
           [[::ui/source
             [::ui/vector [::ui/keyword :movie/director]]]
            [::ui/source {::ui/prefix "-"
                          :data-color "error"}
             [::ui/string "Christopher Nolan"]]])))

  (testing "Orders diffs by path"
    (is (= (->> (render-expanded)
                (lookup/select-one [::ui/card-body :article.diff])
                lookup/children
                (h/strip-attrs #{::ui/actions}))
           [[::ui/source
             [::ui/vector [::ui/keyword :movie/director]]]
            [::ui/source {::ui/prefix "-"
                          :data-color "error"}
             [::ui/string "Christopher Nolan"]]])))

  (testing "Does not allow browsing current version again"
    (is (nil? (->> (render-expanded {:current? true})
                   (lookup/select-one [::ui/card-body ::ui/button]))))))

(deftest render-audit-log
  (testing "Renders revisions in audit log"
    (is (= (->> (audit-log/render-log
                 {:rev 2
                  :history
                  [{:created-at #inst "2025-04-16T16:21:14.000-00:00"
                    :rev 2
                    :val {}
                    :diff [[[:movie/year] :+]]}
                   {:created-at #inst "2025-04-16T14:01:12.000-00:00"
                    :rev 1
                    :val {}
                    :diff [[[:movie/title] :+ "Interstellar"]]}]}
                 {})
                (lookup/select [::ui/card-list ::ui/card])
                count)
           2)))

  (testing "Renders message when audit log is truncated"
    (is (= (->> (audit-log/render-log
                 {:rev 12
                  :history
                  [{:created-at #inst "2025-04-16T16:21:14.000-00:00"
                    :rev 12
                    :val {}
                    :diff [[[:movie/year] :+]]}]}
                 {})
                (lookup/select [::ui/card-list ::ui/card])
                last
                lookup/text)
           "11 older versions have been discarded. Change  :history-limit  to control how history is truncated.")))

  (testing "Renders instructions for values without history tracking"
    (is (= (->> (audit-log/render-log {:dataspex/inspectee "Page data"} {})
                (lookup/select ::ui/card)
                lookup/text)
           (str "Page data isn’t currently set up to track changes. "
                "To turn it on, call inspect with the following options: "
                "dataspex/inspect Page data data :track-changes? true :history-limit 25"))))

  (testing "Tailors instructions to refs"
    (is (= (->> (audit-log/render-log {:dataspex/inspectee "Store" :ref (atom nil)} {})
                (lookup/select ::ui/card)
                lookup/text)
           (str "Store isn’t currently set up to track changes. "
                "To turn it on, call inspect with the following options: "
                "dataspex/inspect Store ref :track-changes? true :history-limit 25")))))
