(ns wh.company.payment.db
  (:require [cljs.spec.alpha :as s]
            [wh.common.specs.payment]
            [wh.common.specs.primitives :as primitives]
            [wh.db :as db]
            [wh.user.db :as user]))

(s/def ::step #{:select-package :pay-confirm :pay-success})
(s/def ::action #{:publish :applications :integrations})
(s/def ::token (s/nilable string?))
(s/def ::error (s/nilable keyword?))
(s/def ::error-message (s/nilable string?))
(s/def ::waiting? boolean?)
(s/def ::coupon-code string?)
(s/def ::coupon-loading? boolean?)
(s/def ::coupon-error (s/nilable string?))
(s/def ::current-coupon (s/nilable :wh.payment/coupon))

(s/def ::description ::primitives/non-empty-string)
(s/def ::amount int?)
(s/def ::proration boolean?)
(s/def ::estimate (s/coll-of (s/keys :req-un [::description
                                              ::amount
                                              ::proration])))

(s/def ::stripe-card-form-enabled? boolean?)
(s/def ::stripe-card-form-error (s/nilable string?))

(s/def ::job-state #{:loading :published})
(s/def ::job-states (s/map-of string? ::job-state))

(def default-db
  {::token                     nil
   ::error                     nil
   ::error-message             nil
   ::stripe-card-form-enabled? true
   ::stripe-card-form-error    nil
   ::estimate                  nil
   ::waiting?                  false
   ::coupon-loading?           false
   ::coupon-error              nil
   ::coupon-code               ""
   ::current-coupon            nil
   ::job-states                {}})
