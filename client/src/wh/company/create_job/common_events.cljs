(ns wh.company.create-job.common-events
  (:require [re-frame.core :refer [reg-event-fx]]
            [wh.db :as db]))

(reg-event-fx
 :publish/try-publish
 db/default-interceptors
 (fn [{db :db} [{:keys [event event-type job-slug]}]]
   (if (db/logged-in? db)
     {:dispatch [event]}
     {:show-auth-popup {:context  (some-> event-type name)
                        :redirect [:job :params {:slug job-slug} :query-params {"publish" true}]}})))
