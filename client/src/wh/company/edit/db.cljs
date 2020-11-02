(ns wh.company.edit.db
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [wh.common.data :as data]
            [wh.common.specs.payment]
            [wh.common.specs.primitives :as p]
            [wh.components.forms.db :as forms]
            [wh.verticals :as verticals]))

(defn job-by-id
  [sub-db id]
  (some #(when (= id (:id %)) %) (::jobs sub-db)))

(def page-size 10)

(def page-selections
  {:company-details "Company Details"
   :payment-details "Payment Details"})

(s/def ::loading? boolean?)
(s/def ::error (s/nilable keyword?))

(s/def ::manager ::data/manager)
(s/def ::vertical verticals/future-job-verticals)
(s/def ::page-selection (set (keys page-selections)))
(s/def ::pending-billing-period :wh.payment/billing-period)

(s/def ::status #{:good :bad})
(s/def ::message string?)
(s/def ::status-container (s/keys :req [::status
                                        ::message]))
(s/def ::update-card-details-status ::status-container)
(s/def ::update-billing-period-status ::status-container)
(s/def ::update-billing-period-loading? boolean?)
(s/def ::update-role-verticals-status ::status-container)
(s/def ::update-role-verticals-loading? boolean)

(s/def ::expanded-role string?)
(s/def ::pending-role-verticals (s/map-of string? verticals/future-job-verticals))
(s/def ::paid-offline-until-loading? boolean?)
(s/def ::paid-offline-until-error (s/nilable string?))

(s/def ::showing-cancel-plan-dialog? boolean?)
(s/def ::cancel-plan-loading? boolean?)
(s/def ::disable-loading? boolean?)

(def fields
  {::name             {:initial "", :validate ::p/non-empty-string :event? false}
   ::logo             {:initial "", :validate ::p/non-empty-string}
   ::description-html {:initial "", :validate ::p/non-empty-string :event? false}
   ::manager          {:initial "", :validate ::manager}
   ::vertical         {:initial verticals/default-vertical, :validate ::vertical}
   ::auto-approve     {:initial false, :validate boolean?}
   ::profile-enabled  {:initial false, :validate boolean?}
   ;; TODO MOVE THIS TO COMPANY PROFILE
   ::videos           {:initial false, :validate boolean?}
   ;;
   ::new-user-name    {:initial "", :validate ::p/non-empty-string}
   ::new-user-email   {:initial "", :validate ::p/email}})

(def company-fields [::name ::logo ::description-html])
(def company-fields-extended (concat company-fields [::manager ::package]))
(def user-fields [::new-user-name ::new-user-email])

(defn check-field
  [form k v]
  (s/valid? (:validate v) (get form k)))

(defn form-errors
  [form extended?]
  (not-empty
   (reduce-kv (fn [a k v] (if (check-field form k v) a (conj a k))) #{}
              (select-keys fields (if extended?
                                    company-fields-extended
                                    company-fields)))))

(defn user-form-errors
  [form]
  (not-empty
   (reduce-kv (fn [a k v] (if (check-field form k v) a (conj a k))) #{}
              (select-keys fields user-fields))))

(defn initial-db
  [db]
  (merge
    (forms/initial-value fields)
    {::loading?                       false
     ::error                          nil
     ::id                             nil
     ::original-company               nil
     ::saving?                        false
     ::user-adding?                   false
     ::suggestions                    []
     ::logo-uploading?                false
     ::pending-billing-period         nil
     ::update-card-details-status     nil
     ::update-billing-period-status   nil
     ::update-role-verticals-status   nil
     ::update-billing-period-loading? false
     ::update-role-verticals-loading? false
     ::show-integration-popup?        false
     ::pending-role-verticals         {}
     ::paid-offline-until-loading?    false
     ::paid-offline-until-error       nil
     ::showing-cancel-plan-dialog?    false
     ::cancel-plan-loading?           false
     ::disable-loading?               false}
    db
    {::coupon-apply-success?          false}))

(def field-names (keys fields))
