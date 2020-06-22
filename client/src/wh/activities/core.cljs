(ns wh.activities.core
  (:require [cljs.loader :as loader]
            [re-frame.core :refer [dispatch-sync reg-event-db]]
            [wh.activities.views :as views]
            [wh.components.activities.queries :as queries]
            [wh.db :as db]
            [wh.graphql-cache :refer [reg-query]]
            [wh.user.db :as user]))

(reg-query :recent_activities queries/recent-activities-query)

(def page-mapping
  {:feed-preview {:page views/preview :can-access? user/admin?}})

(reg-event-db
  ::initialize-page-mapping
  (fn [db _]
    (update db ::db/page-mapping merge page-mapping)))

(dispatch-sync [::initialize-page-mapping])
(dispatch-sync [:graphql/query :recent_activities
                {:activities_tags []}])

(loader/set-loaded! :activities)
