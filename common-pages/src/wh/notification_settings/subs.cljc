(ns wh.notification-settings.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  ::sub-db
  (fn [db _]
    (:wh.notification-settings/sub-db db)))

(reg-sub
  ::frequency-options
  :<- [::sub-db]
  (fn [_sub-db _]
    [{:id "disabled" :label "Never"}
     {:id "daily" :label "Daily"}
     {:id "weekly" :label "Weekly"}
     {:id "monthly" :label "Monthly"}]))

(reg-sub
  ::current-value
  :<- [::sub-db]
  (fn [sub-db _]
    (:wh.notification-settings/current-value sub-db)))

(reg-sub
  ::frequency
  :<- [::current-value]
  (fn [current-value [_ event-type]]
    (get-in current-value [event-type :frequency])))

(reg-sub
  ::user-token
  :<- [::sub-db]
  (fn [sub-db _]
    (:wh.notification-settings/user-token sub-db)))

(reg-sub
  ::updated?
  :<- [:wh/query-params]
  (fn [query-params _]
    (boolean (get query-params "updated"))))

(reg-sub
  ::wrong-arguments?
  :<- [:wh/query-params]
  (fn [query-params _]
    (boolean (get query-params "wrong-arguments"))))
