(ns dataspex.test.source-scenes
  (:require [cljc.java-time.zoned-date-time :as zdt]
            [datascript.core :as d]
            [dataspex.data :as data]
            [dataspex.hiccup :as hiccup]
            [dataspex.test.data :as test-data]
            [dataspex.views :as views]
            [phosphor.icons :as icons]
            [portfolio.replicant :as portfolio :refer-macros [defscene]]
            [time-literals.read-write]))

(time-literals.read-write/print-time-literals-cljs!)

(portfolio/configure-scenes
 {:title "Source views"
  :idx 53})

(defn render-source [o & [opt]]
  (let [opt (assoc opt :dataspex/view views/source)]
    (hiccup/render-source (data/inspect o opt) opt)))

(def fruits
  [{:fruit/id :apple
    :fruit/name "Apple"}
   {:fruit/id :banana
    :fruit/name "Banana"}
   {:fruit/id :pear
    :fruit/name "Pear"}
   {:fruit/id :orange
    :fruit/name "Orange"}
   {:fruit/id :kiwi
    :fruit/name "Kiwi"}
   {:fruit/id :litchi
    :fruit/name "Litchi"}
   {:fruit/id :durian
    :fruit/name "Durian"}])

(defscene source-vector
  (render-source fruits))

(defscene source-list
  (render-source (into '() fruits)))

(defscene source-seq
  (render-source (map identity fruits)))

(defscene map
  (render-source
   {:myapp.ui.command/ctx
    {:location
     {:page-id :pages/my-areas
      :params {}}}}))

(defscene paginated-indefinite-seq-source
  (render-source
   (cycle fruits)
   {:dataspex/path []
    :dataspex/pagination {[] {:page-size 25, :offset 50}}}))

(defscene source-set
  (render-source (set fruits)))

(defscene source-atom
  (render-source (atom fruits)))

(defscene hiccup-indentation
  (render-source
   [:ul.my-4 {:class #{"_grid_mc7vk_1"}
              :data-items "300"}
    [:li {:class #{"_card_mc7vk_1" :relative}}
     [:h2 {:class #{"_heading_mc7vk_1" "_ds-heading_oaddr_1"}
           :data-size "2xs"}
      "Oslo"]]]))

(defscene hiccup
  (prn
   (render-source
    [:div {:class #{"_group_mc7vk_1"}}
     [:h1 {:class #{"_heading_mc7vk_1" "_ds-heading_oaddr_1"}} "My areas"]
     [:p.max-w-prose "Here are some of your areas, enjoy!"]
     nil
     [:ul.my-4 {:class #{"_grid_mc7vk_1"}
                :data-items "300"}
      (list
       [:li {:class #{"_card_mc7vk_1" :relative}}
        [:h2 {:class #{"_heading_mc7vk_1" "_ds-heading_oaddr_1"}
              :data-size "2xs"}
         "Oslo"]
        nil
        [:p.mb-6 1 " restaurants"]
        [:div.p-2.absolute.bottom-0.right-0
         [:myapp.ui.command/button
          {:data-tooltip "Remove"
           :data-color "danger"
           :myapp.ui.command/confirm-text "Sure you want to remove Oslo from your areas?"
           :myapp.ui.command/ctx
           {:db (d/db test-data/conn)
            :location
            {:page-id :pages/my-areas
             :params {}
             :query-params {:county "oslo"}}
            :now-zdt (zdt/now)}
           :myapp.ui.command/command
           {:command/kind :commands/remove-from-my-areas
            :command/data {:area/id "oslo"}}}
          (icons/render (icons/icon :phosphor.fill/check-circle))]]])]
     [:div.mt-8
      [:ui/a {:class #{"_button_mc7vk_1" "_ds-button_oaddr_1"}
              :data-variant "secondary"
              :ui/dest {:page-id :pages/my-areas
                        :query-params {:add "area"}}}
       (icons/render (icons/icon :phosphor.regular/plus))
       "Add area"]]]))
  (render-source
   [:div {:class #{"_group_mc7vk_1"}}
    [:h1 {:class #{"_heading_mc7vk_1" "_ds-heading_oaddr_1"}} "My areas"]
    [:p.max-w-prose "Here are some of your areas, enjoy!"]
    nil
    [:ul.my-4 {:class #{"_grid_mc7vk_1"}
               :data-items "300"}
     (list
      [:li {:class #{"_card_mc7vk_1" :relative}}
       [:h2 {:class #{"_heading_mc7vk_1" "_ds-heading_oaddr_1"}
             :data-size "2xs"}
        "Oslo"]
       nil
       [:p.mb-6 1 " restaurants"]
       [:div.p-2.absolute.bottom-0.right-0
        [:myapp.ui.command/button
         {:data-tooltip "Remove"
          :data-color "danger"
          :myapp.ui.command/confirm-text "Sure you want to remove Oslo from your areas?"
          :myapp.ui.command/ctx
          {:db (d/db test-data/conn)
           :location
           {:page-id :pages/my-areas
            :params {}
            :query-params {:county "oslo"}}
           :now-zdt (zdt/now)}
          :myapp.ui.command/command
          {:command/kind :commands/remove-from-my-areas
           :command/data {:area/id "oslo"}}}
         (icons/render (icons/icon :phosphor.fill/check-circle))]]])]
    [:div.mt-8
     [:ui/a {:class #{"_button_mc7vk_1" "_ds-button_oaddr_1"}
             :data-variant "secondary"
             :ui/dest {:page-id :pages/my-areas
                       :query-params {:add "area"}}}
      (icons/render (icons/icon :phosphor.regular/plus))
      "Add area"]]]))
