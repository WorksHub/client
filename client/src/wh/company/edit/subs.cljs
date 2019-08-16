(ns wh.company.edit.subs
  (:require
    [cljs-time.core :as t]
    [cljs-time.format :as tf]
    [clojure.set :refer [rename-keys]]
    [clojure.spec.alpha :as s]
    [clojure.string :as str]
    [goog.string :as gstring]
    [goog.string.format]
    [re-frame.core :refer [reg-sub]]
    [wh.common.data :refer [managers get-manager-email]]
    [wh.common.errors :as errors]
    [wh.common.specs.primitives :as p]
    [wh.company.edit.db :as edit]
    [wh.db :as db]
    [wh.subs :refer [error-sub-key]]
    [wh.user.subs :as user-subs]))

(reg-sub ::sub-db (fn [db _] (::edit/sub-db db)))

(doseq [field edit/field-names
        :let [sub (keyword "wh.company.edit.subs" (name field))
              db-field (keyword "wh.company.edit.db" (name field))]]
  (reg-sub sub :<- [::sub-db] (fn [db _] (db-field db))))

(def error-msgs {::p/non-empty-string "This field can't be empty."
                 ::p/email "This is not a valid email."
                 ::edit/manager "Please select a valid Manager from the list."})

(defn error-query
  [db k spec]
  (let [value (get db k)]
    (and (contains? (::edit/form-errors db) k)
         (get error-msgs spec "no error msg specified"))))

(doseq [[k {spec :validate}] edit/fields]
  (reg-sub
    (error-sub-key k)
    :<- [::sub-db]
    (fn [db _]
      (error-query db k spec))))

(reg-sub
  ::id
  :<- [::sub-db]
  (fn [db _]
    (::edit/id db)))

(reg-sub
  ::slug
  :<- [::sub-db]
  (fn [db _]
    (::edit/slug db)))

(reg-sub
  ::suggestions
  :<- [::sub-db]
  (fn [db _]
    (mapv #(rename-keys % {:domain :id, :name :label})
          (::edit/suggestions db))))

(reg-sub
  ::logo-uploading?
  :<- [::sub-db]
  (fn [db _]
    (::edit/logo-uploading? db)))

(reg-sub
  ::saving?
  :<- [::sub-db]
  (fn [db _]
    (::edit/saving? db)))

(reg-sub
  ::user-adding?
  :<- [::sub-db]
  (fn [db _]
    (::edit/user-adding? db)))

(reg-sub
  ::users
  :<- [::sub-db]
  (fn [db _]
    (::edit/users db)))

(defn get-server-error-string
  [k]
  (case k
    :duplicate-user "A user with this email address is already registered in our system. Please use a unique email address."
    :unknown-error "An unknown error or timeout has occurred. Please check your connection and try again."
    (errors/upsert-user-error-message k)))

(reg-sub
  ::error
  :<- [::sub-db]
  (fn [db _]
    (some-> (::edit/error db)
            (get-server-error-string))))

(reg-sub
  ::user-error
  :<- [::sub-db]
  (fn [db _]
    (some-> (::edit/user-error db)
            (get-server-error-string))))

(reg-sub
  ::package
  :<- [::sub-db]
  (fn [db _]
    (when-let [p (::edit/package db)]
      (str/capitalize (name p)))))

(reg-sub
  ::package-kw
  :<- [::sub-db]
  (fn [db _]
    (::edit/package db)))

(reg-sub
  ::package-error
  :<- [::sub-db]
  (fn [db _] nil))

(reg-sub
  ::manager
  :<- [::sub-db]
  (fn [db _]
    (::edit/manager db)))

(reg-sub
  ::manager-name-and-email
  :<- [::manager]
  (fn [n _]
    (when-let [email (get-manager-email n)]
      {:email email
       :name n})))

(reg-sub
  ::manager-suggestions
  :<- [::manager]
  (fn [m _]
    (let [m (when m (str/lower-case m))
          results (->> managers
                       (filter (fn [[email manager]] (str/includes? (str/lower-case manager) m)))
                       (map (fn [[email manager]] (hash-map :id manager :label (gstring/format "%s (%s)" manager email))))
                       (take 5)
                       (vec))]
      (if (and (= 1 (count results))
               (= m (some-> results (first) (:label) (str/lower-case))))
        (vector)
        results))))

(reg-sub
  ::show-integration-popup?
  :<- [::sub-db]
  (fn [db _]
    (::edit/show-integration-popup? db)))

(reg-sub
  ::user-notifications
  :<- [::sub-db]
  (fn [db [_ email]]
    (set (get-in db [::edit/integrations :email]))))

(reg-sub
  ::greenhouse-connected?
  :<- [::sub-db]
  (fn [db [_]]
    (get-in db [::edit/integrations :greenhouse :enabled])))

(reg-sub
  ::slack-connected?
  :<- [::sub-db]
  (fn [db [_]]
    (get-in db [::edit/integrations :slack :enabled])))

(reg-sub
  ::some-integrations-connected?
  :<- [::greenhouse-connected?]
  :<- [::slack-connected?]
  :<- [:user/company-connected-github?]
  (fn [integrations [_]]
    (some true? integrations)))

(reg-sub
  ::some-integrations-not-connected?
  :<- [::greenhouse-connected?]
  :<- [::slack-connected?]
  :<- [:user/company-connected-github?]
  (fn [integrations [_]]
    (some (comp not true?) integrations)))

(reg-sub
  ::page-selection
  (fn [db _]
    (or (some-> (get-in db [::db/query-params "page"]) keyword)
        (first (keys edit/page-selections)))))

(reg-sub
  ::page-selections
  (constantly (reduce-kv (fn [a k v] (conj a [k v])) [] edit/page-selections)))

(reg-sub
  ::payment
  :<- [::sub-db]
  (fn [sub-db _]
    (::edit/payment sub-db)))

(reg-sub
  ::coupon
  :<- [::payment]
  (fn [payment _]
    (:coupon payment)))

(reg-sub
 ::payment-expires
 :<- [::payment]
 (fn [payment _]
   (some->> payment
        :expires
        (tf/parse (tf/formatters :date-time))
        (tf/unparse (tf/formatter "DD MMM YYYY")))))

(reg-sub
  ::billing-period
  :<- [::payment]
  (fn [payment _]
    (:billing-period payment)))

(reg-sub
  ::update-card-details-button-enabled?
  :<- [:wh.company.payment.subs/stripe-card-form-error]
  (fn [error _]
    (not error)))

(reg-sub
  ::update-card-details-button-waiting?
  :<- [:wh.company.payment.subs/stripe-card-form-enabled?]
  (fn [enabled? _]
    (false? enabled?)))

(reg-sub
  ::update-card-details-status
  :<- [::sub-db]
  (fn [sub-db _]
    (::edit/update-card-details-status sub-db)))

(reg-sub
  ::pending-billing-period
  :<- [::sub-db]
  :<- [::payment]
  (fn [[sub-db payment] _]
    (let [pbp (::edit/pending-billing-period sub-db)]
      (when (not= pbp (:billing-period payment))
        pbp))))

(reg-sub
  ::update-billing-period-button-enabled?
  :<- [::pending-billing-period]
  (fn [pdp _]
    (boolean pdp)))

(reg-sub
  ::update-billing-period-button-waiting?
  :<- [::sub-db]
  (fn [sub-db]
    (::edit/update-billing-period-loading? sub-db)))

(reg-sub
  ::update-billing-period-status
  :<- [::sub-db]
  (fn [sub-db _]
    (::edit/update-billing-period-status sub-db)))

(reg-sub
  ::loading?
  :<- [::sub-db]
  :<- [::page-selection]
  (fn [[sub-db page] _]
    (case page
      :company-details (when (str/blank? (::edit/name sub-db)) (::edit/loading? sub-db))
      :payment-details (when-not (::edit/payment sub-db)       (::edit/loading? sub-db))
      (::edit/loading? sub-db))))

(reg-sub
  ::invoices
  :<- [::loading?]
  :<- [::sub-db]
  (fn [[loading? sub-db] _]
    (when-not loading?
      (remove (comp (some-fn zero? neg?) :amount) (::edit/invoices sub-db)))))

(reg-sub
  ::error
  :<- [::sub-db]
  (fn [sub-db _]
    (::edit/error sub-db)))

(reg-sub
  ::paid-offline-until
  :<- [::sub-db]
  (fn [db _]
    (when-let [pou (::edit/paid-offline-until db)]
      (tf/unparse (tf/formatter "Do MMMM YYYY")  (tf/parse (tf/formatters :date-time) pou)))))

(reg-sub
  ::paid-offline-until-loading?
  :<- [::sub-db]
  (fn [db _]
    (::edit/paid-offline-until-loading? db)))

(reg-sub
  ::paid-offline-until-error
  :<- [::sub-db]
  (fn [db _]
    (::edit/paid-offline-until-error db)))

(reg-sub
  ::cancel-plan-loading?
  :<- [::sub-db]
  (fn [db _]
    (::edit/cancel-plan-loading? db)))

(reg-sub
  ::showing-cancel-plan-dialog?
  :<- [::sub-db]
  (fn [db _]
    (::edit/showing-cancel-plan-dialog? db)))

(reg-sub
  ::disable-loading?
  :<- [::sub-db]
  (fn [db _]
    (::edit/disable-loading? db)))

(reg-sub
  ::disabled?
  :<- [::sub-db]
  (fn [db _]
    (::edit/disabled db)))

(reg-sub
  ::offer
  :<- [::sub-db]
  (fn [db _]
    (::edit/offer db)))

(reg-sub
  ::coupon-apply-success?
  :<- [::sub-db]
  (fn [db _]
    (::edit/coupon-apply-success? db)))

(reg-sub
  ::has-subscription?
  :<- [::package-kw]
  (fn [package _]
    (and (not= package :explore)
         (not= package :free))))

(reg-sub
  ::deleting-integration?
  :<- [::sub-db]
  (fn [db _]
    (::edit/deleting-integration? db)))

(reg-sub
  ::permissions
  :<- [::sub-db]
  (fn [sub-db _]
    (set (::edit/permissions sub-db))))

(reg-sub
  ::can-cancel-sub?
  :<- [::permissions]
  (fn [perms _]
    (contains? perms "can_cancel_subscription")))
