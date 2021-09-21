(ns wh.components.activities.company-published
  (:require [wh.components.activities.components :as components]
            [wh.components.activities.company :as company]))

(defn card [company actor type opts]
  [components/card type
   [components/header
    [components/company-info actor :company type]
    [components/entity-description :company type]]
   [components/description (if (= type :publish)
                             "Recently published their public profile"
                             "Recently had a lot of views")]
   [company/base-card company actor type opts]])
