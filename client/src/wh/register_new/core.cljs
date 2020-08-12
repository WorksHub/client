(ns wh.register-new.core
  (:require
    [cljs.loader :as loader]
    [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
    [wh.db :as db]
    [wh.register-new.views :as register]
    [wh.register-new.events :as register-events]))

(def page-mapping
  {:register-new register/card-signup})

(reg-event-db ::initialize-page-mapping
  (fn [db _] (update db ::db/page-mapping merge page-mapping)))

(dispatch-sync [::initialize-page-mapping])
(dispatch-sync [::register-events/initialize-db])
(dispatch [:wh.pages.core/unset-loader])

(db/redefine-app-db-spec!)

(loader/set-loaded! :register-new)
