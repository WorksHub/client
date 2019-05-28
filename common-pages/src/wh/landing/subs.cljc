(ns wh.landing.subs
  (:require
    [re-frame.core :refer [reg-sub]]))

(reg-sub
  ::sub-db
  (fn [db _]
    (:wh.homepage.db/sub-db db)))

(reg-sub
  ::jobs
  :<- [::sub-db]
  (fn [sub-db _]
    (:wh.homepage.db/jobs sub-db)))

(reg-sub
  ::blogs
  :<- [::sub-db]
  (fn [sub-db _]
    (:wh.homepage.db/blogs sub-db)))
