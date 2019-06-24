;; Contains apply-related events that need to be there for not-logged-in users.

(ns wh.logged-in.apply.common-events
  (:require
    [re-frame.core :refer [reg-event-fx]]
    [wh.db :as db]))

(reg-event-fx
  :apply/try-apply
  db/default-interceptors
  (fn [{db :db} [job event-type]]
    (if (db/logged-in? db)
      {:load-and-dispatch [:logged-in [:apply/start-apply-for-job job]]}
      {:show-auth-popup {:context  (name event-type)
                         :redirect [:job :params {:slug (:slug job)} :query-params {:apply "true"}]}})))
