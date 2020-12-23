(ns wh.promotions.preview.subs
  (:require [re-frame.core :refer [reg-sub]]
            [wh.re-frame.subs :refer [<sub]]))

(reg-sub
  ::promotions
  :<- [:graphql/result :recent-promotions {}]
  (fn [promotions _]
    (get-in promotions [:query-promotions :promotions])))
