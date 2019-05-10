(ns wh.common.job
  (:require [clojure.string :as str]
            [wh.common.cases :as cases]
            [wh.common.data :as data]
            #?(:cljs [goog.i18n.NumberFormat :as nf])))

(defn format-job-location
  [{:keys [city state country country-code]} remote]
  (cond
    remote country
    (and (not (str/blank? city))
         country-code
         (= "us" (str/lower-case country-code))) (str/join ", " (remove str/blank? [city state country-code]))
    (not (str/blank? city)) (str city ", " country)
    (and (str/blank? city)
         (not (str/blank? state))) (str state ", " country)
    (every? str/blank? [city state]) country))

(defn add-currency-symbol
  [string-salary currency]
  (if-let [pre-fix-symbol (data/currency-symbols currency)]
    (str pre-fix-symbol string-salary)
    (str string-salary " " currency)))

(defn with-suffix [n]
  #?(:clj  (if (< n 1000)
             (str n)
             (let [exp (int (/ (Math/log n)
                               (Math/log 1000)))
                   result (/ n (Math/pow 1000 (double exp)))
                   suffix-character (.charAt "KMGTPE" (- exp 1))]
               (if (= 0.0 (mod result 1))
                 (format "%s%c" (int result) suffix-character)
                 (format "%.1f%c" result suffix-character))))
     :cljs (let [formatter (goog.i18n.NumberFormat. nf/Format.COMPACT_SHORT)]
             (.format formatter n))))

(defn format-salary
  [{:keys [currency min max equity time-period]}]
  (let [min-with-suffix (when min (with-suffix min))
        max-with-suffix (when max (with-suffix max))
        separator (when (and min max) " - ")
        equity-str (when equity " + Equity")
        per-day (when (= "Daily" time-period) " per day")]
    (-> (str min-with-suffix separator max-with-suffix)
        (add-currency-symbol currency)
        (str per-day equity-str))))

(defn format-job-remuneration
  [{:keys [competitive] :as remuneration}]
  (if competitive
    "Competitive"
    (format-salary remuneration)))


(defn translate-job [job]
  (let [{:keys [location remote remuneration] :as job} (cases/->kebab-case job)]
    (assoc job
           :display-location (format-job-location location remote)
           :display-salary (format-job-remuneration remuneration))))
