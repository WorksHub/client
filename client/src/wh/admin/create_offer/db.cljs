(ns wh.admin.create-offer.db
  (:require [cljs.spec.alpha :as s]
            [wh.common.specs.offer :as offer]
            [wh.common.specs.primitives :as p]
            [wh.components.forms.db :as forms]))

(def fields
  {::offer               {:order 1 :initial nil, :validate keyword?}
   ::offer-fixed         {:order 2 :initial nil, :validate :wh.offer/recurring-fee}
   ::offer-percentage    {:order 3 :initial nil, :validate :wh.offer/placement-percentage}})

(defn initial-db [db]
  (merge (forms/initial-value fields)
         {::creating? false
          ::success? false}))
