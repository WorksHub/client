(ns wh.company.payment.subs
  (:require [re-frame.core :refer [reg-sub]]
            [wh.common.data :as data]
            [wh.company.payment.db :as payment]
            [wh.db :as db]
            [wh.user.db :as user]))

(defn payment-step
  [db]
  (get-in db [:wh.db/page-params :step]))

(defn company-id [db]
  (get-in db [::user/sub-db ::user/company-id]))

(defn job-id [db]
  (get (::db/query-params db) "job"))

(defn candidate-id [db]
  (get (::db/query-params db) "candidate"))

(defn action [db]
  (some-> (get (::db/query-params db) "action") keyword))

(defn package [db]
  (some-> (get (::db/query-params db) "package") keyword))

(defn billing-period [db]
  (some-> (get (::db/query-params db) "billing") keyword))

(reg-sub ::db (fn [db _] db))
(reg-sub ::sub-db (fn [db _] (::payment/sub-db db)))

(reg-sub
  ::loading?
  :<- [::db]
  :<- [::sub-db]
  (fn [[db sub-db] _]
    (and (#{:select-package :pay-confirm} (payment-step db))
         (or (not (get-in sub-db [::payment/company :id]))
             (and (get-in db [:wh.db/query-params "job"])
                  (or (not (get-in sub-db [::payment/job :id]))
                      (not (get-in sub-db [::payment/job :valid]))))))))

(reg-sub
  ::company-loading?
  :<- [::sub-db]
  (fn [sub-db _]
    (not (get-in sub-db [::payment/company :id]))))

(reg-sub
  ::waiting?
  :<- [::sub-db]
  (fn [sub-db _]
    (::payment/waiting? sub-db)))

(reg-sub
  ::company-payment
  :<- [::sub-db]
  (fn [sub-db _]
    (get-in sub-db [::payment/company :payment])))

(reg-sub
  ::company-billing-period
  :<- [::company-payment]
  (fn [payment _]
    (some-> (:billing-period payment) keyword)))

(reg-sub
  ::company-card-digits
  :<- [::company-payment]
  (fn [payment _]
    (get-in payment [:card :last-4-digits])))

(reg-sub
  ::has-saved-card-details?
  :<- [::company-payment]
  (fn [payment _]
    (boolean (not-empty (:card payment)))))

(reg-sub
  ::company-coupon
  :<- [::company-payment]
  (fn [payment _]
    (:coupon payment)))

(reg-sub
  ::company-new-offer
  :<- [::sub-db]
  (fn [sub-db _]
    (get-in sub-db [::payment/company :pending-offer])))

(reg-sub
  ::company-offer
  :<- [::sub-db]
  (fn [sub-db _]
    (get-in sub-db [::payment/company :offer])))

(reg-sub
  ::next-invoice
  :<- [::sub-db]
  (fn [sub-db _]
    (get-in sub-db [::payment/company :next-invoice])))

(defn company-package
  [db]
  (get-in db [::payment/sub-db ::payment/company :package]))

(reg-sub
  ::company-package
  :<- [::db]
  (fn [db _]
    (company-package db)))

(reg-sub
 ::company-disabled?
 :<- [::sub-db]
 (fn [sub-db _]
   (get-in sub-db [::payment/company :disabled])))

(reg-sub
  ::billing-period
  (fn [db _]
    (billing-period db)))

(reg-sub
  ::package
  (fn [db _]
    (package db)))

(reg-sub
  ::candidate-id
  (fn [db _]
    (candidate-id db)))

(reg-sub
  ::verticals
  :<- [::sub-db]
  (fn [sub-db _]
    (get-in sub-db [::payment/job :verticals])))

(reg-sub
  ::job-title
  :<- [::sub-db]
  (fn [sub-db _]
    (get-in sub-db [::payment/job :title])))

(reg-sub
  ::job-id
  :<- [::sub-db]
  (fn [sub-db _]
    (get-in sub-db [::payment/job :id])))

(reg-sub
  ::job-slug
  :<- [::sub-db]
  (fn [sub-db _]
    (get-in sub-db [::payment/job :slug])))

(reg-sub
  ::action
  (fn [db _]
    (some-> (get-in db [:wh.db/query-params "action"]) keyword)))

(reg-sub
  ::company-name
  :<- [::sub-db]
  (fn [sub-db _]
    (get-in sub-db [::payment/company :name])))

(reg-sub
  ::company-slug
  :<- [::sub-db]
  (fn [sub-db _]
    (get-in sub-db [::payment/company :slug])))

(reg-sub
  ::has-permission?
  :<- [::sub-db]
  (fn [sub-db [_ permission]]
    (contains? (set (get-in sub-db [::payment/company :permissions])) permission)))

(reg-sub
  ::can-start-free-trial?
  :<- [::has-permission? :can_start_free_trial]
  (fn [has-perm? _]
    has-perm?))

(reg-sub
  ::payment-setup-step
  (fn [db _]
    (payment-step db)))

(reg-sub
  ::error
  :<- [::sub-db]
  :<- [::company-disabled?]
  (fn [[sub-db disabled?] _]
    (if disabled?
      :unauthorised
      (::payment/error sub-db))))

(reg-sub
  ::error-message
  :<- [::sub-db]
  (fn [sub-db _]
    (::payment/error-message sub-db)))

(reg-sub
  ::stripe-card-form-enabled?
  :<- [::sub-db]
  (fn [sub-db _]
    (::payment/stripe-card-form-enabled? sub-db)))

(reg-sub
  ::stripe-card-form-error
  :<- [::sub-db]
  (fn [sub-db _]
    (::payment/stripe-card-form-error sub-db)))

(reg-sub
  ::stripe-public-key
  (fn [db _]
    (:wh.settings/stripe-public-key db)))

(defn upgrading?
  [db]
  (contains? (set (get-in db [::payment/sub-db ::payment/company :permissions])) :can_upgrade))

(reg-sub
  ::upgrading?
  (fn [db _]
    (upgrading? db)))

(reg-sub
  ::estimate
  :<- [::sub-db]
  (fn [sub-db _]
    (::payment/estimate sub-db)))

(reg-sub
  ::existing-billing-period
  (fn [db _]
    (some-> (get (::db/query-params db) "existing-billing") keyword)))

(reg-sub
  ::breakdown?
  (fn [db _]
    (not (= "false" (get (::db/query-params db) "breakdown")))))

(reg-sub
  ::can-press-authorize?
  :<- [::billing-period]
  :<- [::existing-billing-period]
  (fn [[bp ebp] _]
    (not (and ebp bp (= bp ebp)))))

(reg-sub
  ::coupon-loading?
  :<- [::sub-db]
  (fn [sub-db _]
    (::payment/coupon-loading? sub-db)))

(reg-sub
  ::current-coupon
  :<- [::sub-db]
  (fn [sub-db _]
    (::payment/current-coupon sub-db)))

(reg-sub
  ::coupon-error
  :<- [::sub-db]
  (fn [sub-db _]
    (::payment/coupon-error sub-db)))

(reg-sub
  ::coupon-code
  :<- [::sub-db]
  (fn [sub-db _]
    (::payment/coupon-code sub-db)))

(reg-sub
  ::info-message
  :<- [::candidate-id]
  :<- [::job-id]
  :<- [::action]
  (fn [[candidate-id job-id action] _]
    (cond candidate-id
          "You cannot access candidates on your current package. Upgrade in order to unlock access to candidates who have applied to any of your roles \uD83D\uDD12"

          (and job-id (= :applications action))
          "You cannot access applications on your current package. Upgrade in order to unlock access to the applications that have been made to your roles \uD83D\uDD12"

          (= :publish action)
          "You cannot publish any more jobs on your current package. Upgrade in order to publish unlimited jobs across all our platforms \uD83D\uDD12"

          (= :edit action)
          "You cannot edit any more jobs on your current package. Upgrade in order to edit unlimited jobs across all our platforms \uD83D\uDD12"

          (= :integrations action)
          "You cannot use integrations on your current package. Upgrade in order to use all the integrations \uD83D\uDD12"

          :else nil)))

(reg-sub
  ::offer-billing-selection-options
  (fn [_ _]
    (map (fn [[bp {:keys [title discount]}]]
           {:id bp
            :label (str title (when discount (str " (" (* 100 discount) "% off)")))})
         data/billing-data)))

(reg-sub
  ::current-package-data
  :<- [::package]
  :<- [::company-new-offer]
  (fn [[package pending-offer] _]
    (if (= :take_off package)
      (merge (get data/package-data package)
             {:cost (:recurring-fee pending-offer) :per "month"})
      (get data/package-data package))))

(reg-sub
  ::jobs
  :<- [::sub-db]
  (fn [sub-db _]
    (get-in sub-db [::payment/company :jobs :jobs])))

(reg-sub
  ::job-state
  :<- [::sub-db]
  (fn [sub-db [_ job-id]]
    (get-in sub-db [::payment/job-states job-id])))

(reg-sub
  ::has-published-at-least-one-role?
  :<- [::sub-db]
  (fn [sub-db _]
    (contains? (set (vals (get sub-db ::payment/job-states))) :published)))
