(ns wh.register.core
  (:require
    [cljs.loader :as loader]
    [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
    [wh.db :as db]
    [wh.register.views :as register]
    [wh.register.events :as register-events]))

(def page-mapping
  {:register {:page        register/card-signup
              :can-access? (complement db/logged-in?)}})

(reg-event-db ::initialize-page-mapping
  (fn [db _] (update db ::db/page-mapping merge page-mapping)))

(dispatch-sync [::initialize-page-mapping])
(dispatch-sync [::register-events/initialize-db])
(dispatch [:wh.pages.core/unset-loader])

(db/redefine-app-db-spec!)

(loader/set-loaded! :register)
