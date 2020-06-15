(ns wh.landing-new.views
  (:require #?(:cljs [wh.pages.core :refer [load-and-dispatch]])
            [wh.components.attract-card :as attract-card]
            [wh.components.tag-selector.tag-selector :as tag-selector]
            [wh.components.side-card.side-card :as side-cards]
            [wh.components.stat-card :as stat-card]
            [wh.landing-new.subs :as subs]
            [wh.re-frame.subs :refer [<sub]]
            [wh.styles.landing :as styles]
            [wh.util :refer [merge-classes]]))

(defn tag-picker []
  [:div {:class (merge-classes styles/card styles/card--tag-picker)} "Tag picker"])

(defn future-card [cls text]
  [:div {:class (merge-classes styles/card cls)} text])

(def future-cards
  [[styles/card--blog-published "Blog Published"]
   [styles/card--company-stats "Company Stats"]
   [styles/card--issue-started "Issue Started"]
   [styles/card--job-published "Jobs Published"]
   [styles/card--matching-issues "Matching Issues"]
   [styles/card--matching-jobs "Matching Jobs"]])

(defn page []
  (let [blogs (<sub [::subs/top-blogs])
        jobs (<sub [::subs/recent-jobs])
        users (<sub [::subs/top-users])
        issues (<sub [::subs/live-issues])
        companies (<sub [::subs/top-companies])
        tags (<sub [::subs/top-tags])
        logged-in? (<sub [:user/logged-in?])]
    [:div {:class styles/page}
     [:div {:class styles/page__intro}
      [attract-card/intro "functional"]
      [attract-card/contribute logged-in?]
      [attract-card/signin]]
     [:div {:class styles/page__main}
      [:div {:class styles/side-column}
       [tag-selector/card-with-selector tags]
       [side-cards/top-ranking {:blogs blogs
                                :companies companies
                                :default-tab :companies}]
       [side-cards/top-ranking {:blogs       blogs
                                :companies   companies
                                :default-tab :blogs}]]
      (into [:div {:class styles/main-column}
             [stat-card/about-applications]
             [stat-card/about-open-source]
             [stat-card/about-salary-increase]]
            (for [fc future-cards]
              (apply future-card fc)))
      [:div {:class styles/side-column}
       [side-cards/recent-jobs jobs]
       [side-cards/live-issues issues]
       [side-cards/top-ranking-users users]]]]))
