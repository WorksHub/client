(ns wh.register.core
  (:require
    [cljs.loader :as loader]
    [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
    [reagent.core :as reagent]
    [wh.db :as db]
    [wh.register.db :as register-db]
    [wh.register.events :as register-events]
    [wh.register.views :as register]))

(def page-mapping
  {:register register/page})

(reg-event-db
  ::initialize-page-mapping
  (fn [db _]
    (update db ::db/page-mapping merge page-mapping)))

(swap! db/sub-dbs conj ::register-db/sub-db)

(dispatch-sync [::initialize-page-mapping])
(dispatch-sync [::register-events/initialize-db])
(dispatch [:wh.pages.core/unset-loader])

(db/redefine-app-db-spec!)

(loader/set-loaded! :register)
