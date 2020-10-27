(ns wh.profile.update-public.subs
  (:require [re-frame.core :refer [reg-sub]]
            [wh.profile.update-public.db :as profile-update-public]
            [wh.subs :refer [with-unspecified-option]])
  (:require-macros [clojure.core.strint :refer [<<]]))

(reg-sub
  ::sub-db
  (fn [db _]
    (::profile-update-public/sub-db db)))

(reg-sub
  ::editing-profile?
  :<- [::sub-db]
  (fn [db _]
    (boolean (:editing-profile? db))))

(reg-sub
  ::name
  :<- [::sub-db]
  (fn [db _]
    (:name db)))

(reg-sub
  ::avatar-uploading?
  :<- [::sub-db]
  (fn [db _]
    (= (:image-upload-status db) :pending)))

(reg-sub
  ::avatar-uploaded?
  :<- [::sub-db]
  (fn [db _]
    (= (:image-upload-status db) :success)))

(reg-sub
  ::summary
  :<- [::sub-db]
  (fn [db _]
    (:summary db)))

(reg-sub
  ::urls
  :<- [::sub-db]
  (fn [db _]
    (:other-urls db)))

(reg-sub
  ::avatar
  :<- [::sub-db]
  (fn [db _]
    (:image-url db)))

(reg-sub
  ::submit-attempted?
  :<- [::sub-db]
  (fn [db _]
    (:submit-attempted? db)))

(reg-sub
  ::errors
  :<- [::sub-db]
  (fn [db _]
    (profile-update-public/form->errors db)))

(reg-sub
  ::field-error
  :<- [::submit-attempted?]
  :<- [::errors]
  (fn [[submit-attempted? errors] [_ field]]
    (when submit-attempted? (get errors field))))