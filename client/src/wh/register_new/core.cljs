(ns wh.register-new.core
  (:require
    [cljs.loader :as loader]
    [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
    [reagent.core :as reagent]
    [wh.db :as db]
    [wh.register-new.views :as register-new]))

(def page-mapping
  {:register-new register-new/card-signup})

(reg-event-db ::initialize-page-mapping
  (fn [db _] (update db ::db/page-mapping merge page-mapping)))

(dispatch-sync [::initialize-page-mapping])
(dispatch [:wh.pages.core/unset-loader])

(db/redefine-app-db-spec!)

(loader/set-loaded! :register-new)
