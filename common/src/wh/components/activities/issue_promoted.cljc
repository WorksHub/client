(ns wh.components.activities.issue-promoted
  (:require [wh.components.activities.components :as components]
            [wh.components.activities.issue :as issue]))

(defn card [issue actor description type opts]
  [components/card type
   [components/header
    [components/promoter actor]
    [components/entity-description :issue type]]

   [components/quoted-description description]

   [issue/base-card issue (:issue-company issue) type opts]])
