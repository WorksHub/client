(ns wh.landing-new.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  ::sub-db
  (fn [db _]
    (:wh.homepage-new.db/sub-db db)))

(reg-sub
  ::top-companies
  :<- [::sub-db]
  (fn [sub-db _]
    (:top-companies sub-db)))

(reg-sub
  ::top-users
  :<- [::sub-db]
  (fn [sub-db _]
    (:top-users sub-db)))

(reg-sub
  ::live-issues
  :<- [::sub-db]
  (fn [sub-db _]
    (:live-issues sub-db)))

(reg-sub
  ::recent-jobs
  :<- [::sub-db]
  (fn [sub-db _]
    (:recent-jobs sub-db)))

(reg-sub
  ::top-tags
  :<- [::sub-db]
  (fn [sub-db _]
    (:top-tags sub-db)))

(reg-sub
  ::top-blogs
  (fn [db _]
      (get-in (wh.graphql-cache/result db :top-blogs nil) [:top-blogs :results])))
