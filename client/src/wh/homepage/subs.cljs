(ns wh.homepage.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [wh.graphql.jobs]
    [wh.homepage.db :as sub-db]))

(reg-sub
  ::sub-db
  (fn [db _]
    (::sub-db/sub-db db)))

;; This mirrors wh.logged-in.dashboard.subs/jobs and was originally conflated with it
(reg-sub
  ::jobs
  :<- [::sub-db]
  (fn [sub-db _]
    (::sub-db/jobs sub-db)))

(reg-sub
  ::blogs
  :<- [::sub-db]
  (fn [sub-db _]
    (::sub-db/blogs sub-db)))
