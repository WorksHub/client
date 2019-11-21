(ns wh.metrics.subs
  (:require
    [re-frame.core :refer [reg-sub]]))

(reg-sub
  ::data
  (fn [db _]
    (:wh/data db)))

(reg-sub
  ::candidate-data
  :<- [::data]
  (fn [data _]
    (-> data :data :time-periods)))

(reg-sub
  ::company-data
  :<- [::data]
  (fn [data _]
    (-> data :company-data reverse)))

(reg-sub
  ::time-period
  :<- [::data]
  (fn [data _]
    (-> data :time-period)))

(reg-sub
  ::total-registrations
  :<- [::data]
  (fn [data _]
    (-> data :data :base)))
