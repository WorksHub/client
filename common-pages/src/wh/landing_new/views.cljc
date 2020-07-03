(ns wh.landing-new.views
  (:require #?(:cljs [wh.pages.core :refer [load-and-dispatch]])
            [wh.components.activities.article-published :as article-published]
            [wh.components.activities.company-published :as company-published]
            [wh.components.activities.components :as activities]
            [wh.components.activities.issue-published :as issue-published]
            [wh.components.activities.job-published :as job-published]
            [wh.components.activities.job-published :as job-published]
            [wh.components.attract-card :as attract-card]
            [wh.components.newsletter :as newsletter]
            [wh.components.side-card.side-card :as side-cards]
            [wh.components.side-card.side-card-mobile :as side-cards-mobile]
            [wh.components.stat-card :as stat-card]
            [wh.components.tag-selector.tag-selector :as tag-selector]
            [wh.landing-new.subs :as subs]
            [wh.re-frame.subs :refer [<sub]]
            [wh.styles.landing :as styles]
            [wh.util :as util]))

(defmulti activity-card (juxt :verb :object-type))

(defmethod activity-card [:publish "company"] [activity]
  [company-published/card (:object activity) :publish])

(defmethod activity-card [:publish "job"] [activity]
  [job-published/card (:object activity) :publish])

(defmethod activity-card [:publish "issue"] [activity]
  [issue-published/card (:object activity) :publish])

(defmethod activity-card [:publish "article"] [activity]
  [article-published/card (:object activity) :publish])

(defmethod activity-card [:highlight "company"] [activity]
  [company-published/card (:object activity) :highlight])

(defmethod activity-card [:highlight "job"] [activity]
  [job-published/card (:object activity) :highlight])

(defmethod activity-card [:highlight "issue"] [activity]
  [issue-published/card (:object activity) :highlight])

(defmethod activity-card [:highlight "article"] [activity]
  [article-published/card (:object activity) :highlight])

(defmethod activity-card :default [_activity]
  nil)

(defn tag-picker []
  [:div (util/smc styles/card styles/card--tag-picker) "Tag picker"])

(defn split-into-3-groups [elms]
  (let [size (count elms)
        group-size (cond
                     (< size 2) 1
                     (< size 6) 2
                     :else (quot size 3))]
    [(take group-size elms)
     (->> elms
          (drop group-size)
          (take group-size))
     (drop (* 2 group-size) elms)]))

(defn page []
  (let [blogs               (<sub [::subs/top-blogs])
        blogs-loading?      (<sub [::subs/top-blogs-loading?])
        companies           (<sub [::subs/top-companies])
        companies-loading?  (<sub [::subs/top-companies-loading?])
        users               (<sub [::subs/top-users])
        users-loading?      (<sub [::subs/top-users-loading?])
        jobs                (<sub [::subs/recent-jobs])
        jobs-loading?       (<sub [::subs/recent-jobs-loading?])
        issues              (<sub [::subs/recent-issues])
        issues-loading?     (<sub [::subs/recent-issues-loading?])
        tags                (<sub [::subs/top-tags])
        tags-loading?       (<sub [::subs/top-tags-loading?])
        activities          (<sub [::subs/recent-activities])
        activities-loading? (<sub [::subs/recent-activities-loading?])
        logged-in?          (<sub [:user/logged-in?])
        vertical            (<sub [:wh/vertical])
        query-params        (<sub [:wh/query-params])
        selected-tags       (<sub [::subs/selected-tags])
        company?            (<sub [:user/company?])]
    [:div (util/smc styles/page)
     (when-not logged-in?
       [attract-card/all-cards {:logged-in? logged-in?
                                :vertical   vertical}])
     [:div (util/smc styles/page__main)
      [:div (util/smc styles/side-column styles/side-column--left)
       [tag-selector/card-with-selector tags tags-loading?]
       [side-cards/top-ranking {:blogs        blogs
                                :companies    companies
                                :default-tab  :companies
                                :redirect     :feed
                                :logged-in?   logged-in?
                                :query-params query-params
                                :loading?     (or blogs-loading? companies-loading?)}]]
      (if activities-loading?
        (into [:div {:class styles/main-column}
               (for [i (range 6)]
                 ^{:key i}
                 [activities/card-skeleton])])
        (let [[group1 group2 group3] (split-into-3-groups activities)
              display-stat-card? (and (> (count activities) 6) (not logged-in?))]
          (into [:div {:class styles/main-column}
                 [side-cards-mobile/top-content {:jobs   jobs
                                                 :blogs  blogs
                                                 :issues issues}]
                 (when (and (not activities-loading?)
                            (= 0 (count activities)))
                   [activities/card-not-found selected-tags])
                 (for [activity group1] ^{:key (:id activity)} [:div (activity-card activity)])
                 (when display-stat-card? [stat-card/about-applications])
                 [newsletter/newsletter {:logged-in? logged-in?
                                         :type       :landing}]
                 (when display-stat-card? [stat-card/about-open-source])
                 (for [activity group2] ^{:key (:id activity)} [:div (activity-card activity)])
                 (when display-stat-card? [stat-card/about-salary-increase])
                 (for [activity group3] ^{:key (:id activity)} [:div (activity-card activity)])])))
      [:div {:class styles/side-column}
       [side-cards/recent-jobs jobs jobs-loading? company?]
       [side-cards/recent-issues issues issues-loading? company?]
       [side-cards/top-ranking-users users users-loading?]]]]))
