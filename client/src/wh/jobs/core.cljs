(ns wh.jobs.core
  (:require [cljs.loader :as loader]
            [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
            [reagent.core :as reagent]
            [wh.db :as db]
            [wh.job.events :as job-events]
            [wh.job.views :as job]
            [wh.jobs.jobsboard.events :as jobsboard-events]
            [wh.jobsboard.views :as jobsboard]))

(def page-mapping
  {:job            job/page
   :jobsboard      jobsboard/jobsboard-page
   :pre-set-search jobsboard/preset-search-page})

(reg-event-db
  ::initialize-page-mapping
  (fn [db _]
    (update db ::db/page-mapping merge page-mapping)))

(dispatch-sync [::initialize-page-mapping])
(dispatch [:wh.pages.core/unset-loader])

;; load extra symbols
(let [symbol-filename "symbols/job.svg"]
  (when-not (.getElementById js/document (str "load-icons-" symbol-filename))
    (js/loadSymbols symbol-filename)))

(loader/set-loaded! :jobs)
