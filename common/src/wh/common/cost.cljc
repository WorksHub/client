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

(defn calculate-monthly-cost
  [cost discount {:keys [discount-amount discount-percentage]}]
  (let [monthly (- cost (* cost (or discount 0)))]
    (cond (and discount-amount (pos? discount-amount))
          (- monthly discount-amount)
          (and discount-percentage (pos? discount-percentage))
          (- monthly (* monthly (/ discount-percentage 100)) )
          :else monthly)))
