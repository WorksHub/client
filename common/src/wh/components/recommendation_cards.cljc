(ns wh.components.recommendation-cards
  (:require [wh.components.issue :as issue]
            [wh.components.job :as job]
            [wh.components.cards :as cards]))

(defn jobs
  [{:keys [jobs
           company-id
           admin?
           instant-apply?
           logged-in?]}]
  (when (seq jobs)
    [:div.recommendation-cards
     [:h2.recommendation-cards__title "Recommended Jobs"]
     (for [job jobs]
       ^{:key (:id job)}
        [job/job-card job {:logged-in?        logged-in?
                           :small?            true
                           :user-has-applied? instant-apply?
                           :user-is-company?  (not (nil? company-id))
                           :user-is-owner?    (or admin? (= company-id (:company-id job)))}])]))

(defn issues
  [{:keys [issues]}]
  (when (seq issues)
    [:div.recommendation-cards
     [:h2.recommendation-cards__title "Recommended Issues"]
     (for [issue issues]
       ^{:key (:id issue)}
       [issue/issue-card issue {:small? true}])]))

(defn blogs
  [{:keys [blogs]}]
  (when (seq blogs)
    [:div.recommendation-cards
     [:h2.recommendation-cards__title "Recommended Articles"]
     (for [blog blogs]
       ^{:key (:id blog)}
       [cards/blog-row blog])]))
