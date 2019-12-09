(ns wh.logged-in.apply.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [wh.logged-in.apply.db :as apply]))

(reg-sub ::sub-db (fn [db _] (::apply/sub-db db)))

(reg-sub
  ::display-chatbot?
  :<- [::sub-db]
  (fn [db _]
    (::apply/job db)))

(reg-sub
  ::step-taken?
  :<- [::sub-db]
  (fn [db [_ step]]
    (contains? (::apply/steps-taken db) step)))

(reg-sub
  ::current-step
  :<- [::sub-db]
  (fn [db _]
    (::apply/current-step db)))

(reg-sub
  ::updating?
  :<- [::sub-db]
  (fn [db _]
    (::apply/updating? db)))

(reg-sub
  ::submit-success?
  :<- [::sub-db]
  (fn [db _]
    (::apply/submit-success? db)))

(reg-sub
  ::error
  :<- [::sub-db]
  (fn [db _]
    (::apply/error db)))

(def error-mapping
  {:no-matching-job            "Whoops, looks like you attempted to apply for a nonexistent job!"
   :application-already-exists "You have already applied for this job!"
   :incorrect-user-type        "You should be a registered candidate to apply."
   :incomplete-profile         "Looks like you have an incomplete profile. We need your name, email, current location and either an uploaded CV or a link to one."
   :unknown-error              "There was an error submitting your application, please try again or contact us if problems persist."})

(def rejection-mapping
  {:no-visa "Unfortunately it looks like you do not meet the visa requirements for the job \uD83D\uDE14"
   :unknown-reason "Unfortunately your application does not meet the requirement for the job \uD83D\uDE14"})

(reg-sub
  ::error-message
  :<- [::error]
  (fn [error _]
    (or (error-mapping error) (:unknown-error error-mapping))))

(reg-sub
  ::rejection-message
  :<- [::sub-db]
  (fn [db _]
    (let [rejection (::apply/rejection db)]
      (or (rejection-mapping (:reason rejection)) (:unknown-reason rejection-mapping)))))

(reg-sub
  ::cv-upload-failed?
  :<- [::sub-db]
  (fn [db _]
    (::apply/cv-upload-failed? db)))

(reg-sub
  ::update-name-failed?
  :<- [::sub-db]
  (fn [db _]
    (::apply/name-update-failed? db)))

(reg-sub
  ::update-visa-failed?
  :<- [::sub-db]
  (fn [db _]
    (::apply/visa-update-failed? db)))

(reg-sub
  ::update-current-location-failed?
  :<- [::sub-db]
  (fn [db _]
    (::apply/current-location-update-failed? db)))

(defn location-label [loc]
  (when loc
    (str (:location/city loc) ", " (:location/country loc))))

;;

(reg-sub
  ::current-location-label
  :<- [::sub-db]
  (fn [db _]
    (or (::apply/current-location-text db)
        (location-label (::apply/current-location db)))))

(defn location->suggestion
  [x]
  {:label (location-label x)
   :id    x})

(reg-sub
  ::current-location-suggestions
  :<- [::sub-db]
  (fn [db [_ i]]
    (mapv location->suggestion (::apply/current-location-suggestions db))))

(reg-sub
  ::company-managed?
  :<- [::sub-db]
  (fn [db _]
    (::apply/company-managed? db)))

(reg-sub
  ::company-name
  :<- [::sub-db]
  (fn [db _]
    (::apply/company-name db)))
