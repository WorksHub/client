(ns wh.blogs.learn.views
  (:require
    [re-frame.core :refer [dispatch]]
    [wh.blogs.learn.events :as events]
    [wh.blogs.learn.subs :as subs]
    [wh.components.cards :refer [blog-card blog-row]]
    [wh.components.pagination :refer [pagination]]
    [wh.interop :as interop]
    [wh.components.pods.candidates :as candidate-pods]
    [wh.routes :as routes]
    [wh.components.job :refer [job-card]]
    [wh.slug :as slug]
    [wh.components.tag :as tag]
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
          #?(:clj (when-not logged-in?
                    (interop/on-click-fn
                      (interop/show-auth-popup :contribute [:contribute])))
             :cljs {:on-click #(dispatch (if logged-in?
                                           [:wh.events/nav :contribute]
                                           [:wh.events/contribute]))}) "Write Article"])]]]))

(defn promoted-jobs []
  (let [jobs         (<sub [::subs/recommended-jobs])
        logged-in?   (<sub [:user/logged-in?])
        has-applied? (some? (<sub [:user/applied-jobs]))
        company-id   (<sub [:user/company-id])
        admin?       (<sub [:user/admin?])]
    (when-not
      (= (count jobs) 0)
      [:section.promoted-jobs
       [:h2 "Promoted Jobs"]
       (for [job jobs]
         ^{:key (:id job)}
         [:div
          [job-card job {:logged-in?        logged-in?
                         :small?            true
                         :user-has-applied? has-applied?
                         :user-is-company?  (not (nil? company-id))
                         :user-is-owner?    (or admin? (= company-id (:company-id job)))}]])])))

(defn page []
  (let [blogs (<sub [::subs/all-blogs])
        tag (<sub [:wh/page-param :tag])
        tags (<sub [::subs/tagbox-tags])
        tags-section [:section.split-content-section
                      (tag/strs->tag-list :a tags
                        {:f #(assoc % :href (routes/path :learn-by-tag :params {:tag (slug/slug+encode (:label %))}))})]]
    [:div.main.articles-page
     (learn-header)
     [:div.split-content
      [:div.split-content__main
       (for [blog blogs]
         ^{:key (:id blog)}
         [blog-row  blog])
       [pagination
        (<sub [::subs/current-page])
        (<sub [::subs/pagination])
        (if tag :learn-by-tag :learn)
        (<sub [:wh/query-params])
        (when tag {:tag tag})]]
      [:div.split-content__side
       tags-section
       [candidate-pods/candidate-cta]
       [promoted-jobs]]]]))



