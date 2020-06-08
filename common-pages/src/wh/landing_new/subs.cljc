(ns wh.landing-new.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  ::sub-db
  (fn [db _]
    (:wh.homepage-new.db/sub-db db)))

(reg-sub
  ::top-blogs
  :<- [::sub-db]
  (fn [sub-db _]
    (:top-blogs sub-db)))
