(ns wh.logged-in.personalised-jobs.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [wh.graphql.jobs :as jobs]
    [wh.logged-in.personalised-jobs.db :as personalised-jobs]
    ))

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
    (->> (::personalised-jobs/jobs sub-db)
         (jobs/add-interactions liked-jobs applied-jobs))))

;; unsure what to call these, as :user/liked-jobs is a sub etc, 
;; but as they're namespaced separated , it isn't an IMMEDIATE issue...

(reg-sub
  ::liked-jobs
  :<- [::jobs]
  (fn [all-jobs]
    (filterv :liked all-jobs)))

(reg-sub
 ::applied-jobs
 :<- [::jobs]
 (fn [all-jobs]
   (filterv :applied all-jobs)))

(reg-sub
 ::recommended-jobs
 :<- [::personalised-jobs]
 (fn [personalised-jobs]
   (::personalised-jobs/jobs personalised-jobs)))

(reg-sub
  ::show-load-more?
  :<- [::personalised-jobs]
  (fn [sub-db _]
    (::personalised-jobs/show-load-more? sub-db)))
