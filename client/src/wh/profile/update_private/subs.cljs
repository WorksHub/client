(ns wh.profile.update-private.subs
  (:require [re-frame.core :refer [reg-sub]]
            [wh.common.data :as data]
            [wh.profile.update-private.db :as profile-update-private]
            [wh.profile.update-public.db :as profile-update-public]
            [wh.subs :refer [with-unspecified-option]])
  (:require-macros [clojure.core.strint :refer [<<]]))

(reg-sub
  ::sub-db
  (fn [db _]
    (::profile-update-private/sub-db db)))

(reg-sub
  ::editing-profile?
  :<- [::sub-db]
  (fn [db _]
    (boolean (:editing-profile? db))))

(reg-sub
  ::email
  :<- [::sub-db]
  (fn [db _]
    (:email db)))

(reg-sub
  ::phone
  :<- [::sub-db]
  (fn [db _]
    (:phone db)))

(reg-sub
  ::job-seeking-statuses
  (with-unspecified-option (vals data/job-seeking-status->name) "None"))

(reg-sub
  ::job-seeking-status
  :<- [::sub-db]
  (fn [db _]
    (:job-seeking-status db)))

(reg-sub
  ::visa-status-options
  ;; because visa options is a #{set} and we want :other to be the last option
  (let [options-without-other (->> data/visa-options
                                   (remove #(= % "Other"))
                                   sort
                                   vec)
        options (conj options-without-other "Other")]
    (->> options
         (map #(hash-map :label % :value %))
         constantly)))

(reg-sub
  ::visa-status
  :<- [::sub-db]
  (fn [db _]
    (or (:visa-status db) #{})))

(defn role-type->value [role-type]
  (case role-type
    ;; CH4172, lacinia doesn't support spaces in enums so we need to convert
    "Full time" "Full_time"
    role-type))

(reg-sub
  ::role-type-options
  (->> data/role-types
       (map #(hash-map :label % :value (role-type->value %)))
       constantly))

(reg-sub
  ::role-types
  :<- [::sub-db]
  (fn [db _]
    (:role-types db)))

(reg-sub
  ::prefer-remote?
  :<- [::sub-db]
  (fn [db _]
    (boolean (:remote db))))

(reg-sub
  ::show-visa-status-other?
  :<- [::visa-status]
  (fn [visa-status]
    (profile-update-private/other-visa-status-present? visa-status)))

(reg-sub
  ::visa-status-other
  :<- [::sub-db]
  (fn [db _]
    (:visa-status-other db)))

(reg-sub
  ::currency-options
  (with-unspecified-option data/currencies "Currency"))

(reg-sub
  ::time-period-options
  (with-unspecified-option data/time-periods "Period"))

(reg-sub
  ::salary-min
  :<- [::sub-db]
  (fn [db]
    (get-in db [:salary :min])))

(reg-sub
  ::salary-currency
  :<- [::sub-db]
  (fn [db]
    (get-in db [:salary :currency])))

(reg-sub
  ::salary-time-period
  :<- [::sub-db]
  (fn [db]
    (get-in db [:salary :time-period])))

(defn location-label [loc]
  (when loc
    (<< "~(:location/city loc), ~(:location/country loc)")))

(reg-sub
  ::current-location-label
  :<- [::sub-db]
  (fn [profile _]
    (or (:current-location-text profile)
        (location-label (:current-location profile)))))

(defn location->suggestion
  [x]
  {:label (location-label x), :id x})

(reg-sub
  ::current-location-suggestions
  :<- [::sub-db]
  (fn [profile _]
    (mapv location->suggestion (:current-location-suggestions profile))))

(reg-sub
  ::location-suggestions
  :<- [::sub-db]
  (fn [db [_ i]]
    (->> (get-in db [:location-suggestions i])
         (mapv location->suggestion))))

(reg-sub
  ::preferred-location-labels
  :<- [::sub-db]
  (fn [db _]
    (->> (:preferred-locations db)
         (mapv #(if (string? %) % (location-label %))))))

(reg-sub
  ::submit-attempted?
  :<- [::sub-db]
  (fn [db _]
    (:submit-attempted? db)))

(reg-sub
  ::errors
  :<- [::sub-db]
  (fn [db _]
    (profile-update-private/form->errors db)))

(reg-sub
  ::field-error
  :<- [::submit-attempted?]
  :<- [::errors]
  (fn [[submit-attempted? errors] [_ field]]
    (when submit-attempted? (get errors field))))
