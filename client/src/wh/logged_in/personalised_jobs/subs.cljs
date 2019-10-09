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

(reg-sub
  ::show-load-more?
  :<- [::personalised-jobs]
  (fn [sub-db _]
    (::personalised-jobs/show-load-more? sub-db)))
