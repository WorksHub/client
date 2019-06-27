(ns wh.company.profile.core
  (:require
    [cljs.loader :as loader]
    [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
    [reagent.core :as reagent]
    [wh.company.profile.db :as profile-db]
    [wh.company.profile.events :as profile-events]
    [wh.company.profile.views :as profile]
    [wh.db :as db]))

(def page-mapping
  {:company profile/page})

(reg-event-db
  ::initialize-page-mapping
  (fn [db _]
    (update db ::db/page-mapping merge page-mapping)))

(swap! db/sub-dbs conj ::profile-db/sub-db)

(dispatch-sync [::initialize-page-mapping])
(dispatch [:wh.pages.core/unset-loader])

(db/redefine-app-db-spec!)

(loader/set-loaded! :company-profile)
