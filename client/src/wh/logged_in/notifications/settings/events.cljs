(ns wh.logged-in.notifications.settings.events
  (:require [re-frame.core :refer [path reg-event-db reg-event-fx]]
            [wh.common.cases :as cases]
            [wh.common.errors :refer [upsert-user-error-message]]
            [wh.common.graphql-queries :as graphql]
            [wh.common.keywords :as keywords]
            [wh.db :as db]
            [wh.logged-in.notifications.settings.db :as settings]
            [wh.pages.core :refer [on-page-load]]
            [wh.user.db :as user]
            [wh.util :as util]))

(def settings-interceptors (into db/default-interceptors
                                 [(path ::settings/sub-db)]))

(def settings-query
  {:venia/queries [[:me [[:notificationSettings [[:matchingJob [:frequency]]]]]]]})

(reg-event-fx
  ::fetch-settings
  settings-interceptors
  (fn [_ _]
    {:graphql {:query settings-query
               :on-success [::fetch-settings-success]
               :on-failure [::fetch-settings-failure]}}))

(reg-event-db
  ::fetch-settings-success
  db/default-interceptors
  (fn [db [resp]]
    (let [data (cases/->kebab-case (:data resp))
          settings (get-in data [:me :notification-settings])]
      (-> db
          (assoc-in [::user/sub-db ::user/notification-settings] settings)
          (assoc-in [::settings/sub-db :frequencies] settings)))))

(reg-event-fx
  ::fetch-settings-failure
  db/default-interceptors
  (fn [_ _]
    {:dispatch [:error/set-global "There was an error fetching your settings." [::fetch-settings]]}))

(defn settings-from-query-params [db]
  (let [query-params (::db/query-params db)
        matching-params (select-keys query-params (map name settings/types))]
    (into {}
          (map (fn [[k v]] [(keyword k) {:frequency v}]))
          matching-params)))

(reg-event-db
  ::initialize-db
  db/default-interceptors
  (fn [db _]
    (assoc-in db [::settings/sub-db :frequencies]
              (merge (get-in db [::user/sub-db ::user/notification-settings])
                     (settings-from-query-params db)))))

(reg-event-db
  ::edit-frequency
  settings-interceptors
  (fn [db [event-type new-value]]
    (assoc-in db [:frequencies event-type :frequency] new-value)))

(reg-event-fx
  ::save
  db/default-interceptors
  (fn [{db :db} _]
    {:db (update db ::settings/sub-db merge
                 {:saving? true
                  :save-status nil})
     :graphql {:query graphql/update-user-mutation--approval
               :variables {:update_user (keywords/transform-keys
                                         {:id (get-in db [::user/sub-db ::user/id])
                                          :notification-settings (get-in db [::settings/sub-db :frequencies])})}
               :on-success [::save-success]
               :on-failure [::save-failure]}}))

(reg-event-db
  ::save-success
  db/default-interceptors
  (fn [db _]
    (-> db
        (assoc-in [::settings/sub-db :saving?] false)
        (assoc-in [::settings/sub-db :save-status] {:status :good, :message "Settings saved successfully."})
        (assoc-in [::user/sub-db ::user/notification-settings]
                  (get-in db [::settings/sub-db :frequencies])))))

(reg-event-db
  ::save-failure
  settings-interceptors
  (fn [db [resp]]
    (assoc db
           :saving? false
           :save-status {:status :bad, :message (-> resp util/gql-errors->error-key upsert-user-error-message)})))

(defmethod on-page-load :notifications-settings [db]
  (let [query-param-settings? (seq (settings-from-query-params db))]
    [(when-not query-param-settings?
       [::fetch-settings])
     [::initialize-db]
     (when query-param-settings?
       [::save])]))
