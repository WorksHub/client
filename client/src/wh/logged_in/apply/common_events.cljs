;; Contains apply-related events that need to be there for not-logged-in users.

(ns wh.logged-in.apply.common-events
  (:require [re-frame.core :refer [reg-event-fx]]
            [wh.db :as db]))

(reg-event-fx
  :apply/try-apply
  db/default-interceptors
  (fn [{db :db} [job event-type]]
    (if (db/logged-in? db)
      (let [apply-source (or (some-> event-type name)
                             (get-in db [::db/query-params "apply_source"]))]
        {:load-and-dispatch [:logged-in [:apply/start-apply-for-job job apply-source]]})
      {:show-auth-popup {:context  (some-> event-type name)
                         :redirect [:job :params {:slug (:slug job)} :query-params {"apply" true "apply_source" (name event-type)}]}})))
