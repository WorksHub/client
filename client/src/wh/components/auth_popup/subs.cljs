(ns wh.components.auth-popup.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [wh.components.auth-popup.db :as popup]))

(reg-sub ::sub-db (fn [db _] (::popup/sub-db db)))

(reg-sub
  ::visible?
  :<- [::sub-db]
  (fn [db _]
    (::popup/visible? db)))

(reg-sub
  ::context
  :<- [::sub-db]
  (fn [db _]
    (::popup/context db)))
