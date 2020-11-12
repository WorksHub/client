(ns wh.profile.section-admin.subs
  (:require [re-frame.core :refer [reg-sub]]
            [wh.common.keywords :as keywords]
            [wh.profile.section-admin.db :as section-admin-db]))

(reg-sub
  ::db
  (fn [db _]
    (::section-admin-db/sub-db db)))

(reg-sub
  ::profile
  (fn [db _]
    (-> db
        :wh.logged-in.profile.db/sub-db
        keywords/strip-ns-from-map-keys)))

(reg-sub
  ::profile-loaded?
  :<- [::profile]
  (fn [profile _]
    (boolean (:id profile))))

;; alias standard subscription to incorporate query params
(reg-sub
  ::admin-view?
  :<- [:user/admin?]
  :<- [:wh/query-param "type"]
  (fn [[admin? type] _]
    (and admin? (not (= type "public")))))

(reg-sub
  ::hs-url
  :<- [::profile]
  (fn [profile _]
    (:hubspot-profile-url profile)))

(reg-sub
  ::applications
  :<- [::profile]
  (fn [profile _]
    (:applied profile)))

(reg-sub
  ::liked-jobs
  :<- [::profile]
  (fn [profile _]
    (:likes profile)))

(reg-sub
  ::approval-info
  :<- [::profile]
  (fn [profile _]
    (:approval profile)))

(reg-sub
  ::updating-status
  :<- [::db]
  (fn [db _]
    (section-admin-db/updating-status db)))
