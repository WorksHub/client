(ns wh.blogs.learn.views
  (:require
    [re-frame.core :refer [dispatch]]
    [wh.blogs.learn.events :as events]
    [wh.blogs.learn.subs :as subs]
    [wh.components.cards :refer [blog-card blog-row]]
    [wh.components.pagination :refer [pagination]]
    [wh.components.issue :as issue]
    [wh.components.carousel :refer [carousel]]
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

(defn recommended-jobs []
  (let [jobs         (<sub [::subs/recommended-jobs])
        logged-in?   (<sub [:user/logged-in?])
        has-applied? (some? (<sub [:user/applied-jobs]))
        company-id   (<sub [:user/company-id])
        admin?       (<sub [:user/admin?])]
    (when-not (= (count jobs) 0)
      [:section.recommendation
       [:h2 "Recommended Jobs"]
       (for [job jobs]
         ^{:key (:id job)}
         [:div
          [job-card job {:logged-in?        logged-in?
                         :small?            true
                         :user-has-applied? has-applied?
                         :user-is-company?  (not (nil? company-id))
                         :user-is-owner?    (or admin? (= company-id (:company-id job)))}]])])))

(defn recommended-issues []
  (let [issues (<sub [::subs/recommended-issues])]
    (when-not (= (count issues) 0)
      [:section.recommendation
       [:h2 "Recommended Issues"]
       [:div.issues__list
        (for [issue issues]
          ^{:key (:id issue)}
          [issue/issue-card issue {:small? true}])]])))

(defn tag-picker [tags]
  [:section.split-content-section.tag-picker
   (tag/strs->tag-list :a tags
    {:f #(assoc % :href (routes/path :learn-by-tag :params {:tag (slug/slug+encode (:label %))}))})])

(def carousel-size 6)

(defn recommended-issues-mobile []
  (let [issues (take carousel-size (<sub [::subs/recommended-issues]))
        steps (for [issue issues]
                ^{:key (:id issue)}
                [issue/issue-card issue {:small? true}])]
    (when-not (= (count issues) 0)
      [:div.recommendation.recommendation--mobile.recommendation--issues.is-hidden-desktop
       [:h2.recommendation__title "Recommended Issues"]
       [carousel steps {:arrows? true
                        :arrows-position :bottom}]])))

(defn recommended-jobs-mobile []
  (let [jobs         (take carousel-size (<sub [::subs/recommended-jobs]))
        logged-in?   (<sub [:user/logged-in?])
        has-applied? (some? (<sub [:user/applied-jobs]))
        company-id   (<sub [:user/company-id])
        admin?       (<sub [:user/admin?])
        steps (for [job jobs]
                ^{:key (:id job)}
                [job-card job {:logged-in?        logged-in?
                               :small?            true
                               :user-has-applied? has-applied?
                               :user-is-company?  (not (nil? company-id))
                               :user-is-owner?    (or admin? (= company-id (:company-id job)))}])]
    (when-not (= (count jobs) 0)
      [:div.recommendation.recommendation--mobile.is-hidden-desktop
       [:h2.recommendation__title "Recommended Jobs"]
       [carousel steps {:arrows? true
                        :arrows-position :bottom}]])))

(defn page []
  (let [blogs (<sub [::subs/all-blogs])
        tag (<sub [:wh/page-param :tag])
        tags (<sub [::subs/tagbox-tags])
        ch-size (quot (count blogs) 3)
        [ch1 ch2 ch3] [(take ch-size blogs)
                       (->> blogs
                            (drop ch-size)
                            (take ch-size))
                       (drop (* 2 ch-size) blogs)]]
    [:div.main.articles-page
     (learn-header)
     [:div.split-content
      [:div.split-content__main.articles-page__blogs
       [:div.is-hidden-desktop
        [tag-picker tags]]
       (for [blog ch1]
         ^{:key (:id blog)}
         [blog-row  blog])
       (when (> ch-size 1)
         [recommended-jobs-mobile])
       (for [blog ch2]
         ^{:key (:id blog)}
         [blog-row  blog])
       (when (> ch-size 1)
         [recommended-issues-mobile])
       (for [blog ch3]
         ^{:key (:id blog)}
         [blog-row  blog])
       [pagination
        (<sub [::subs/current-page])
        (<sub [::subs/pagination])
        (if tag :learn-by-tag :learn)
        (<sub [:wh/query-params])
        (when tag {:tag tag})]]
      [:div.split-content__side.is-hidden-mobile
       [tag-picker tags]
       [candidate-pods/candidate-cta]
       [recommended-jobs]
       [recommended-issues]]]]))



