(ns wh.landing-new.views
  (:require #?(:cljs [wh.pages.core :refer [load-and-dispatch]])
            [wh.components.activities.article-published :as article-published]
            [wh.components.activities.company-published :as company-published]
            [wh.components.activities.components :as activities]
            [wh.components.activities.issue-published :as issue-published]
            [wh.components.activities.issue-started :as issue-started]
            [wh.components.activities.job-published :as job-published]
            [wh.components.attract-card :as attract-card]
            [wh.components.newsletter :as newsletter]
            [wh.components.side-card.side-card :as side-cards]
            [wh.components.side-card.side-card-mobile :as side-cards-mobile]
            [wh.components.stat-card :as stat-card]
            [wh.components.tag-selector.tag-selector :as tag-selector]
            [wh.landing-new.components :as components]
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

(defmethod activity-card [:start_issue "issue"] [activity]
  [issue-started/card (:object activity) :start_issue])

(defmethod activity-card :default [_activity]
  nil)

(defn tag-picker []
  [:div (util/smc styles/card styles/card--tag-picker) "Tag picker"])

(def additional-info-threshold 6)
(defn split-into-4-groups [elms]
  (if (> (count elms) additional-info-threshold)
    [(take 2 elms)
     (->> elms
          (drop 2)
          (take 2))
     (->> elms
          (drop 4)
          (take 2))
     (drop 6 elms)]
    [elms nil nil nil]))

(defn left-column [logged-in? jobs jobs-loading?
                   show-recommendations? issues issues-loading? company?
                   admin? blogs-loading? users users-loading? blogs page]
  (let [companies          (<sub [::subs/top-companies])
        companies-loading? (<sub [::subs/top-companies-loading?])
        tags               (<sub [::subs/top-tags])
        tags-loading?      (<sub [::subs/top-tags-loading?])
        query-params       (<sub [:wh/query-params])]
    [:div (util/smc styles/side-column styles/side-column--left)
     [tag-selector/card-with-selector
      tags tags-loading? {:admin? admin? :company? company?}]
     [side-cards/improve-your-recommendations logged-in?]
     [attract-card/contribute logged-in? :side-column]
     [:div (merge (util/smc styles/side-column styles/tablet-only)
                  (util/ts "recommended-jobs-mobile"))
      [side-cards/jobs {:jobs                  jobs
                        :jobs-loading?         jobs-loading?
                        :company?              company?
                        :show-recommendations? show-recommendations?}]
      [side-cards/osi-how-it-works]
      [side-cards/recent-issues issues issues-loading? company?]
      [side-cards/top-ranking-users users users-loading?]]
     [side-cards/top-ranking {:blogs        blogs
                              :companies    companies
                              :default-tab  :companies
                              :redirect     page
                              :logged-in?   logged-in?
                              :query-params query-params
                              :loading?     (or blogs-loading? companies-loading?)}]
     (when-not logged-in?
       [attract-card/signin :side-column])]))

(defn right-column [jobs jobs-loading? show-recommendations? issues issues-loading?
                    company? users users-loading?]
  [:div (merge (util/smc styles/side-column)
               (util/ts "recommended-jobs-desktop"))
   [side-cards/jobs {:jobs                  jobs
                     :loading?              jobs-loading?
                     :company?              company?
                     :show-recommendations? show-recommendations?}]
   [side-cards/osi-how-it-works]
   [side-cards/recent-issues issues issues-loading? company?]
   [side-cards/top-ranking-users users users-loading?]])

(defn activities-loading [candidate?]
  (into [:div {:class styles/main-column}
         (when candidate?
           [:<>
            [components/user-dashboard]
            [:hr]])

         (for [i (range 6)]
           ^{:key i}
           [activities/card-skeleton])]))

(defn activities-list
  [logged-in? candidate? jobs blogs issues show-recommendations?
   vertical activities activities-loading? selected-tags not-enough-activities?]

  (let [[group1 group2 group3 group4] (split-into-4-groups activities)
        display-additional-info?      (and (> (count activities)
                                              additional-info-threshold)
                                           (not logged-in?))
        older-than                    (:id (last activities))
        newer-than                    (:id (first activities))]
    [:div {:class styles/main-column}
     (when candidate?
       [:<>
        [components/user-dashboard]
        [:hr]])

     [side-cards-mobile/top-content
      {:jobs                  jobs
       :blogs                 blogs
       :issues                issues
       :show-recommendations? show-recommendations?}]
     [attract-card/intro vertical :main-column]
     (when (and (not activities-loading?)
                (= 0 (count activities)))
       [activities/card-not-found selected-tags])
     (for [activity group1]
       ^{:key (:id activity)} [:div (activity-card activity)])
     (when display-additional-info?
       [stat-card/about-applications vertical])
     (for [activity group2]
       ^{:key (:id activity)} [:div (activity-card activity)])
     (when (and display-additional-info? (not logged-in?))
       [attract-card/signin])
     (when display-additional-info?
       [stat-card/about-open-source vertical])
     (for [activity group3]
       ^{:key (:id activity)} [:div (activity-card activity)])
     (when display-additional-info?
       [stat-card/about-salary-increase vertical])
     [newsletter/newsletter {:logged-in? logged-in?
                             :type       :landing}]
     (for [activity group4]
       ^{:key (:id activity)} [:div (activity-card activity)])

     (when not-enough-activities?
       [:<>
        [:hr]
        [side-cards/improve-feed-recommendations]])

     [components/prev-next-buttons newer-than older-than]]))

(defn main-column [logged-in? jobs blogs issues show-recommendations? vertical]
  (let [activities             (<sub [::subs/recent-activities])
        activities-loading?    (<sub [::subs/recent-activities-loading?])
        selected-tags          (<sub [::subs/selected-tags])
        candidate?             (<sub [::subs/candidate?])
        not-enough-activities? (<sub [::subs/not-enough-activities?])]
    (if activities-loading?
      [activities-loading candidate?]

      [activities-list
       logged-in? candidate? jobs blogs issues show-recommendations? vertical
       activities activities-loading? selected-tags not-enough-activities?])))


(defn page []
  (let [logged-in?            (<sub [:user/logged-in?])
        show-recommendations? (<sub [:user/has-recommendations?])
        blogs                 (<sub [::subs/top-blogs])
        blogs-loading?        (<sub [::subs/top-blogs-loading?])
        users                 (<sub [::subs/top-users])
        users-loading?        (<sub [::subs/top-users-loading?])
        jobs                  (<sub [(if show-recommendations?
                                       ::subs/recommended-jobs
                                       ::subs/recent-jobs)])
        jobs-loading?         (<sub [(if show-recommendations?
                                       ::subs/recommended-jobs-loading?
                                       ::subs/recent-jobs-loading?)])
        issues                (<sub [::subs/recent-issues])
        issues-loading?       (<sub [::subs/recent-issues-loading?])
        vertical              (<sub [:wh/vertical])
        page                  (<sub [:wh/page])
        company?              (<sub [:user/company?])
        admin?                (<sub [:user/admin?])]

    [:div (util/smc styles/page)
     (when-not logged-in?
       [attract-card/all-cards {:logged-in? logged-in?
                                :vertical   vertical}])
     [:div (util/smc styles/page__main)
      [left-column
       logged-in? jobs jobs-loading?
       show-recommendations? issues issues-loading? company?
       admin? blogs-loading? users users-loading? blogs page]

      [main-column
       logged-in? jobs blogs issues show-recommendations? vertical]

      [right-column
       jobs jobs-loading? show-recommendations? issues
       issues-loading? company? users users-loading?]]]))
