(ns wh.logged-in.notifications.settings.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [wh.logged-in.notifications.settings.db :as sub-db]))

(reg-sub
  ::sub-db
  (fn [db _]
    (::sub-db/sub-db db)))

(reg-sub
  ::frequency
  :<- [::sub-db]
  (fn [sub-db [_ event-type]]
    (get-in sub-db [:frequencies event-type :frequency])))

(reg-sub
  ::saving?
  :<- [::sub-db]
  (fn [sub-db _]
    (:saving? sub-db)))

(reg-sub
  ::save-enabled?
  :<- [::saving?]
  :<- [::frequency :matching-job]
  (fn [[saving? matching-job] _]
    (boolean (and matching-job (not saving?)))))

(reg-sub
  ::save-status
  :<- [::sub-db]
  (fn [sub-db _]
    (:save-status sub-db)))
