(ns wh.profile.core
  (:require
    [cljs.loader :as loader]
    [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
    [wh.profile.views :as profile]
    [wh.profile.events]
    [wh.db :as db]))

(def page-mapping
  {:user profile/page})

(reg-event-db
  ::initialize-page-mapping
  (fn [db _]
    (update db ::db/page-mapping merge page-mapping)))

(dispatch-sync [::initialize-page-mapping])

(loader/set-loaded! :user)
