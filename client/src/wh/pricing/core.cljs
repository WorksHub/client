(ns wh.pricing.core
  (:require
    [cljs.loader :as loader]
    [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
    [wh.db :as db]
    [wh.landing.views :as landing]
    [wh.pricing.views :as pricing]))

(def page-mapping
  {:pricing pricing/page
   :employers landing/page})

(reg-event-db
  ::initialize-page-mapping
  (fn [db _]
    (update db ::db/page-mapping merge page-mapping)))

(dispatch-sync [::initialize-page-mapping])

(loader/set-loaded! :pricing)
