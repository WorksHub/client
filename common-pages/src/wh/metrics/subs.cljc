(ns wh.metrics.subs
  (:require
    #?(:clj [wh.url :as url])
    [re-frame.core :refer [reg-sub]]
    [wh.verticals :as verticals]))

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

(reg-sub
  ::vertical-options
  (fn [db _]
    (mapv (fn [vertical]
           {:id (keyword vertical) :label vertical :url (url/get-url vertical :metrics)})
         (concat ["www"] verticals/ordered-job-verticals))))
