(ns wh.company.views
  (:require [goog.i18n.NumberFormat :as nf]))

(def dollar-formatter (goog.i18n.NumberFormat. nf/Format.CURRENCY))
(defn int->dollars
  ([i]
   (int->dollars i {}))
  ([i {:keys [cents?]
       :or {cents? false}}]
   (if cents?
     (let [[dollars cents] (rest (re-find #"([\-0-9]+)([0-9]{2})" (str i)))]
       (str (int->dollars dollars) "." cents))
     (let [d (.format dollar-formatter i)]
       (subs d 0 (- (count d) 3)))))) ;; remove decimals

(defn double->dollars
  [i]
  (.format dollar-formatter i))
