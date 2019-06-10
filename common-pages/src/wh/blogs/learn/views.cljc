(ns wh.blogs.learn.views
  (:require
    [re-frame.core :refer [dispatch]]
    [wh.blogs.learn.events :as events]
    [wh.blogs.learn.subs :as subs]
    [wh.components.cards :refer [blog-card]]
    [wh.components.pagination :refer [pagination]]
    [wh.re-frame.subs :refer [<sub]]))

(defn learn-header
  []
  (let [logged-in? (<sub [:user/logged-in?])]
    [:div
     [:h1 (<sub [::subs/header])]
     [:div.spread-or-stack
      [:h3 (<sub [::subs/sub-header])]
      [:div.has-bottom-margin
       (when (<sub [::subs/show-contribute?])
         [:button#learn_contribute.button.learn--contribute-button
          {:on-click #(dispatch (if logged-in?
                                  [:wh.events/nav :contribute]
                                  [:wh.events/contribute]))} "Contribute"])]]]))

(defn page []
  (into
   [:div.main
    (learn-header)]
   (let [parts (partition-all 3 (<sub [::subs/all-blogs]))
         tag (<sub [:wh/page-param :tag])]
     (if (seq parts)
       (conj (vec (for [part parts]
                    (into [:div.columns]
                          (for [blog part]
                            [:div.column.is-4
                             [blog-card blog]]))))
             [pagination
              (<sub [::subs/current-page])
              (<sub [::subs/pagination])
              (if tag :learn-by-tag :learn)
              (<sub [:wh/query-params])
              (when tag {:tag tag})])
       [[:p "No learning resources found."]]))))
