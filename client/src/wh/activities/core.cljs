(ns wh.activities.core
  (:require [cljs.loader :as loader]
            [re-frame.core :refer [dispatch-sync reg-event-db]]
            [wh.activities.queries :as queries]
            [wh.activities.views :as views]
            [wh.db :as db]
            [wh.graphql-cache :refer [reg-query]]
            [wh.user.db :as user]))

(reg-query :all_activities queries/all-activities-query)

(def page-mapping
  {:feed-preview {:page views/preview :can-access? user/admin?}})

(reg-event-db
  ::initialize-page-mapping
  (fn [db _]
    (update db ::db/page-mapping merge page-mapping)))

(dispatch-sync [::initialize-page-mapping])
(dispatch-sync [:graphql/query :all_activities {}])

(loader/set-loaded! :activities)
