(ns wh.components.activities.job-promoted
  (:require [wh.components.activities.components :as components]
            [wh.components.activities.job :as job]))

(defn card [job actor description type opts]
  [components/card type
   [components/header
    [components/promoter actor]
    [components/entity-description :job type]]

   [components/quoted-description description]

   [job/base-card job (:job-company job) type opts]])
