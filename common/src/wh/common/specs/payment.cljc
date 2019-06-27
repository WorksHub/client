(ns wh.common.specs.payment
  (:require
    [#?(:clj clojure.spec.alpha
        :cljs cljs.spec.alpha) :as s]
    [#?(:clj clojure.spec.gen.alpha
        :cljs cljs.spec.gen.alpha) :as gen]
    [wh.common.specs.date]
    [wh.common.specs.primitives :as p]))

(defn four-digits?
  [s]
  (and (string? s)
       (= 4 (count s))
       (every? #?(:clj #(<= (int \0) (int %) (int \9))
                  :cljs #(<= (.charCodeAt \0 0) (.charCodeAt % 0) (.charCodeAt \9 0))) s)))

(def plan-periods #{:one :six :twelve}) ;; months

(s/def :wh.payment.card/last-4-digits (s/with-gen
                                        four-digits?
                                        (fn [] (gen/fmap
                                                (partial apply str)
                                                (gen/vector (gen/one-of (map gen/return (range 10))) 4)))))

(s/def :wh.payment.card.expiry/month (set (range 1 13)))
(s/def :wh.payment.card.expiry/year  (set (range 2018 2118)))
(s/def :wh.payment.card/expiry (s/keys :req-un [:wh.payment.card.expiry/month
                                                :wh.payment.card.expiry/year]))
(s/def :wh.payment.card/brand ::p/non-empty-string)
(s/def :wh.payment/card (s/keys :req-un [:wh.payment.card/expiry
                                         :wh.payment.card/last-4-digits
                                         :wh.payment.card/brand]))
(s/def :wh.payment/customer-id ::p/non-empty-string)
(s/def :wh.payment/subscription-id ::p/non-empty-string)
(s/def :wh.payment/plan-id ::p/non-empty-string)
(s/def :wh.payment/billing-period plan-periods)
(s/def :wh.payment/expires :wh/date)
(s/def :wh.payment/trial-until pos-int?)

(s/def :wh.payment/token ::p/non-empty-string)
(s/def :wh.payment/idempotency-seed ::p/non-empty-string)

(s/def :wh.payment.coupon/description ::p/non-empty-string)
(s/def :wh.payment.coupon/code ::p/non-empty-string)
(s/def :wh.payment.coupon/duration #{:once :forever})
(s/def :wh.payment.coupon/discount-amount pos-int?)
(s/def :wh.payment.coupon/discount-percentage ::p/percentage)

(s/def :wh.payment/coupon
  (s/keys :req-un [:wh.payment.coupon/description
                   :wh.payment.coupon/code
                   :wh.payment.coupon/duration]
          :opt-un [:wh.payment.coupon/discount-amount
                   :wh.payment.coupon/discount-percentage]))

(s/def :wh/payment (s/keys :req-un [:wh.payment/card
                                    :wh.payment/customer-id
                                    :wh.payment/billing-period]
                           :opt-un [:wh.payment/subscription-id
                                    :wh.payment/plan-id
                                    :wh.payment/expires
                                    :wh.payment/trial-until
                                    :wh.payment/coupon]))

(s/def :wh/payment-details (s/keys :req-un [:wh.payment/card]
                                   :opt-un [:wh.payment/billing-period
                                            :wh.payment/expires
                                            :wh.payment/coupon]))

(s/def :wh.payment/with-billing-period
  (s/merge :wh/payment (s/keys :req-un [:wh.payment/billing-period])))

(s/def :wh.payment.invoice/date string?)
(s/def :wh.payment.invoice/url string?)
(s/def :wh.payment.invoice/amount int?)
(s/def :wh.payment/invoice (s/keys :req-un [:wh.payment.invoice/date
                                            :wh.payment.invoice/url
                                            :wh.payment.invoice/amount]
                                   :opt-un [:wh.payment/coupon]))
(s/def :wh.payment/invoices (s/coll-of :wh.payment/invoice))

(s/def :wh.payment.invoice-estimate/description ::p/non-empty-string)
(s/def :wh.payment.invoice-estimate/amount int?)
(s/def :wh.payment.invoice-estimate/proration boolean?)
(s/def :wh.payment.invoice-estimate.period/start :wh.payment.invoice/date)
(s/def :wh.payment.invoice-estimate.period/end :wh.payment.invoice/date)
(s/def :wh.payment.invoice-estimate/period (s/keys :req-un [:wh.payment.invoice-estimate.period/start
                                                            :wh.payment.invoice-estimate.period/end]))
(s/def :wh.payment/invoice-estimate (s/keys :req-un [:wh.payment.invoice-estimate/description
                                                     :wh.payment.invoice-estimate/amount
                                                     :wh.payment.invoice-estimate/proration
                                                     :wh.payment.invoice-estimate/period]))
