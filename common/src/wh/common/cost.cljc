(ns wh.common.cost
  #?(:cljs (:require [goog.i18n.NumberFormat :as nf])
     :clj (:import [java.text NumberFormat]
                   [java.util Locale])))

(def dollar-formatter #?(:cljs (goog.i18n.NumberFormat. nf/Format.CURRENCY)
                         :clj (NumberFormat/getInstance (Locale/US))))

(defn double->dollars
  [i]
  #?(:cljs (.format dollar-formatter i)
     :clj  (do
             (.setMinimumFractionDigits dollar-formatter 2)
             (str "$" (.format dollar-formatter i)))))

(defn int->dollars
  ([i]
   (int->dollars i {}))
  ([i {:keys [cents?]
       :or {cents? false}}]
   (if cents?
     (let [[dollars cents] (rest (re-find #"([\-0-9]+)([0-9]{2})" (str i)))]
       (str (int->dollars dollars) "." cents))
     (let [d (double->dollars i)]
       (subs d 0 (- (count d) 3)))))) ;; remove decimals

(defn- apply-coupon
  [amount {:keys [discount-amount discount-percentage]}]
  (cond (and discount-amount (pos? discount-amount))
        (- amount discount-amount)
        (and discount-percentage (pos? discount-percentage))
        (- amount (* amount (/ discount-percentage 100)))
        :else amount))

(defn calculate-monthly-cost
  [cost discount coupon]
  (let [monthly (- cost (* cost (or discount 0)))]
    (if (= (:duration coupon) :forever)
      (apply-coupon monthly coupon)
      monthly)))

(defn calculate-initial-payment
  [num-months monthly coupon]
  (cond->
    {:before-coupon (* num-months monthly)}
    (= (:duration coupon) :once)
    (assoc :after-coupon (apply-coupon (* num-months monthly) coupon))))

(defn calculate-next-charge-from-invoice
  [{:keys [amount]} number cost discount coupon]
  (let [cost-minus-next (* (dec number)
                           (calculate-monthly-cost cost discount coupon))
        cost-next (calculate-monthly-cost (/ amount 100)
                                          discount coupon)]
    (+ cost-next cost-minus-next)))
