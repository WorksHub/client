(ns wh.profile.update-private.db
  (:require [clojure.string :as str]))

(defn form->errors
  [form]
  (let [salary? (not (nil? (get-in form [:salary :min])))
        currency-missing? (nil? (get-in form [:salary :currency]))
        time-period-missing? (nil? (get-in form [:salary :time-period]))
        salary-incorrect? (and salary? (or currency-missing? time-period-missing?))]
    (cond-> {}
            (str/blank? (:email form))
            (assoc :email "Please fill in your email")
            salary-incorrect?
            (assoc :salary "Please fill in currency and time period"))))

(defn other-visa-status-present? [visa-statuses]
  (contains? visa-statuses "Other"))

(defn initialize-preferred-locations
  "we want preferred locations list to be a vector,
  without initialization it will become a map because
  (= (assoc nil 0 1) {0 1}) but with initialization (= (assoc [] 0 1) [1])"
  [preferred-locations]
  (if (nil? preferred-locations)
    []
    preferred-locations))
