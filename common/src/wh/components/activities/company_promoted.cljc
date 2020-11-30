(ns wh.components.activities.company-promoted
  (:require [wh.components.activities.company :as company]
            [wh.components.activities.components :as components]))

(defn card [company actor type opts]
  [components/card type
   [components/header
    [components/promoter actor]
    [components/entity-description :company type]]

   [components/quoted-description ""]

   [company/base-card company actor type opts]])
