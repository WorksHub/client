(ns wh.components.activities.interview-requests
  (:require [wh.components.activities.company :as company]
            [wh.components.activities.components :as components]
            [wh.components.activities.job :as job]
            [wh.common.emoji :as emoji]))

(defn card [company actor type _opts]
  (let [interviews-count          (->> (:feed-jobs company)
                                       (map :recent-interviews-count)
                                       (reduce +))
        interview-requests-period "last week"]
    [components/card type
     [components/header
      [components/company-info actor :company :interview-requests]
      [components/entity-description :company-hiring :interview-requests]]
     [components/description {:type :cropped}
      [:span (:name company) " scheduled "
       [components/->interviews-display-value {:interviews-count  interviews-count
                                               :interviews-period interview-requests-period
                                               :bold-count?       true}]
       emoji/fire]]
     [company/jobs-list-card company
      (for [job (:feed-jobs company)]
        [job/details (assoc job :interview-requests-period interview-requests-period) :interview-requests actor])]]))
