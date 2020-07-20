(ns wh.landing-page.core
  (:require
    [cljs.loader :as loader]
    [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
    [wh.db :as db]
    [wh.landing-new.events] ;; include this so events are registered
    [wh.landing-new.views :as landing]))

(def page-mapping
  {:feed landing/page})

(reg-event-db
  ::initialize-page-mapping
  (fn [db _]
    (update db ::db/page-mapping merge page-mapping)))

(dispatch-sync [::initialize-page-mapping])

(loader/set-loaded! :landing-page)
