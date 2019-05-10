(ns wh.pricing.core
  (:require
    [cljs.loader :as loader]
    [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
    [wh.components.pricing :as pricing]
    [wh.db :as db]))

(def page-mapping
  {:pricing pricing/page})

(reg-event-db
  ::initialize-page-mapping
  (fn [db _]
    (update db ::db/page-mapping merge page-mapping)))

(dispatch-sync [::initialize-page-mapping])

(loader/set-loaded! :pricing)
