(ns wh.components.error.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [wh.components.error.db :as error]))

(reg-sub ::sub-db (fn [db _] (::error/sub-db db)))

(reg-sub
  ::message
  :<- [::sub-db]
  (fn [db _]
    (::error/message db)))

(reg-sub
  ::type
  :<- [::sub-db]
  (fn [db _]
    (name (::error/type db))))

(reg-sub
  ::retry-event
  :<- [::sub-db]
  (fn [db _]
    (::error/retry-event db)))