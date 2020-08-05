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
  :<- [:wh/vertical]
  :<- [:user/admin?]
  (fn [[data vertical admin?] _]
    (let [vert (if (or (not admin?)
                       (and admin? (= vertical "www")))
                 "all"
                 vertical)]
      (->> data :data :time-periods
           (map (fn [period]
                  (-> period
                      (update :registrations get vert)
                      (update :approvals get vert)
                      (update :outstanding get vert))))))))

(reg-sub
  ::company-data
  :<- [::data]
  (fn [data _]
    (-> data :company-data reverse)))

(reg-sub
  ::time-period
  :<- [::data]
  (fn [data _]
    (or (-> data :time-period)
        (-> data :data :time-periods first))))

(reg-sub
  ::total-registrations
  :<- [::data]
  (fn [data _]
    (-> data :data :base)))

(reg-sub
  ::vertical-options
  (fn [db _]
    #?(:clj ;; vertical options only available in SSR anyway?
       (mapv (fn [vertical]
               {:id (keyword vertical) :label vertical :url (url/get-url vertical :metrics)})
             (concat ["www"] verticals/ordered-job-verticals)))))
