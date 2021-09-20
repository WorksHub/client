(ns wh.logged-in.personalised-jobs.subs
  (:require [re-frame.core :refer [reg-sub]]
            [wh.common.job :as job]
            [wh.logged-in.personalised-jobs.db :as personalised-jobs]))

(reg-sub
  ::personalised-jobs
  (fn [db _]
    (::personalised-jobs/sub-db db)))

(reg-sub
  ::jobs
  :<- [::personalised-jobs]
  :<- [:user/liked-jobs]
  :<- [:user/applied-jobs]
  (fn [[sub-db liked-jobs applied-jobs] _]
    (job/add-interactions liked-jobs applied-jobs (::personalised-jobs/jobs sub-db))))

;; unsure what to call these, as :user/liked-jobs is a sub etc, 
;; but as they're namespaced separated , it isn't an IMMEDIATE issue...

(reg-sub
  ::liked-jobs
  :<- [::jobs]
  (fn [all-jobs]
    (filterv :liked all-jobs)))

(reg-sub
  ::selected-job-id
  :<- [:wh/query-param "job-id"]
  (fn [selected-job-id]
    selected-job-id))

(reg-sub
  ::applied-jobs
  :<- [::jobs]
  :<- [::selected-job-id]
  (fn [[all-jobs selected-job-id] _]
    (->> (filterv :applied all-jobs)
         (sort-by #(= (:id %) selected-job-id))
         reverse)))

(reg-sub
  ::recommended-jobs
  :<- [::personalised-jobs]
  (fn [personalised-jobs]
    (job/sort-by-user-score
      (::personalised-jobs/jobs personalised-jobs))))

(reg-sub
  ::show-load-more?
  :<- [::personalised-jobs]
  (fn [sub-db _]
    (::personalised-jobs/show-load-more? sub-db)))
