(ns wh.landing-page.core
  (:require
    [cljs.loader :as loader]
    [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
    [wh.db :as db]
    [wh.landing-new.events] ;; include this so events are registered
    [wh.landing-new.views :as landing]))

(def page-mapping
  {:homepage-new landing/page})

(reg-event-db
  ::initialize-page-mapping
  (fn [db _]
    (update db ::db/page-mapping merge page-mapping)))

(dispatch-sync [::initialize-page-mapping])

;; load extra symbols
(let [symbol-filename "symbols/activities.svg"]
  (when-not (.getElementById js/document (str "load-icons-" symbol-filename))
    (js/loadSymbols symbol-filename)))

(loader/set-loaded! :landing-page)
