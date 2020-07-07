(ns wh.admin.activities.events
  (:require [wh.components.activities.queries :as queries]
            [wh.graphql-cache :refer [reg-query]]
            [wh.pages.core :refer [on-page-load]]))

(reg-query :recent_activities queries/recent-activities-query)

(defmethod on-page-load :feed-preview [_db]
  [[:graphql/query :recent_activities
    {:activities_tags []}]])
