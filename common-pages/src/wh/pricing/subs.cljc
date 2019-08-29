(ns wh.pricing.subs
  (:require
    [re-frame.core :refer [reg-sub]]))

(reg-sub
  ::can-start-free-trial?
  (fn [db _]
    (let [company (get-in db [:wh.user.db/sub-db :wh.user.db/company])]
      (contains? (:permissions company) :can_start_free_trial))))
