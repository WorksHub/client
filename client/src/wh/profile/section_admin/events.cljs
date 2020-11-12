(ns wh.profile.section-admin.events
  (:require
    [re-frame.core :refer [dispatch path reg-event-db reg-event-fx]]
    [wh.admin.queries :as admin-queries]
    [wh.db :as db]
    [wh.profile.section-admin.db :as section-admin-db]))

(def profile-interceptors (into db/default-interceptors
                                [(path ::section-admin-db/sub-db)]))

(reg-event-fx
  ::set-approval-status-failure
  profile-interceptors
  (fn [{db :db} [opts]]
    {:db      (section-admin-db/unset-updating-status db)
     :dispatch [:error/set-global "Failed to set user status."
                [::set-approval-status opts]]}))

(reg-event-fx
  ::set-approval-status-success
  profile-interceptors
  (fn [{db :db} [{:keys [fetch-profile]}]]
    {:db      (section-admin-db/unset-updating-status db)
     :dispatch fetch-profile}))

(reg-event-fx
  ::set-approval-status
  profile-interceptors
  (fn [{db :db} [{:keys [id status] :as opts}]]
    {:db      (section-admin-db/set-updating-status db status)
     :graphql {:query      admin-queries/set-approval-status-mutation
               :variables  {:id id :status status}
               :on-success [::set-approval-status-success opts]
               :on-failure [::set-approval-status-failure opts]}}))

