(ns wh.components.activities.issue-published
  (:require [wh.components.activities.components :as components]
            [wh.components.activities.issue :as issue]))

(defn card [{:keys [body] :as issue} actor type opts]
  [components/card type
   [components/header
    [components/company-info actor :issue type]
    [components/entity-description :issue type]]
   [components/description {:type :cropped} body]
   [issue/base-card issue actor type opts]])
