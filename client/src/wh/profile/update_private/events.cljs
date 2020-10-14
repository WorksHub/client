(ns wh.profile.update-private.events
  (:require [re-frame.core :refer [path reg-event-db reg-event-fx]]
            [wh.common.graphql-queries :as graphql]
            [wh.common.keywords :as keywords]
            [wh.db :as db]
            [wh.logged-in.profile.db :as profile]
            [wh.logged-in.profile.location-events :as location-events]
            [wh.profile.update-private.db :as profile-update-private]
            [wh.user.db :as user]
            [wh.util :as util]))

(def profile-update-interceptors
  (into db/default-interceptors [(path ::profile-update-private/sub-db)]))

(def profile-keys [:id :phone :email :visa-status :visa-status-other
                   :role-types :job-seeking-status :salary :remote
                   :preferred-locations :current-location])

(reg-event-db
  ::open-form
  db/default-interceptors
  (fn [db _]
    (let [profile-sub-db (::profile/sub-db db)
          profile-update-sub-db (::profile-update-private/sub-db db)
          fields-to-copy (-> profile-sub-db
                             keywords/strip-ns-from-map-keys
                             (select-keys profile-keys))
          form-initialized? (:initial-values profile-update-sub-db)]
      (cond-> db
              :always
              (assoc-in [::profile-update-private/sub-db :editing-profile?] true)
              (not form-initialized?)
              (update ::profile-update-private/sub-db
                      #(-> %
                           (merge fields-to-copy)
                           (assoc :initial-values fields-to-copy)
                           (update :preferred-locations profile-update-private/initialize-preferred-locations)))))))

(reg-event-db
  ::close-form
  profile-update-interceptors
  (fn [db _]
    (assoc db :editing-profile? false)))

(reg-event-db
  ::edit-email
  profile-update-interceptors
  (fn [db [email]]
    (assoc db :email email)))

(reg-event-db
  ::edit-phone
  profile-update-interceptors
  (fn [db [phone]]
    (assoc db :phone phone)))

(reg-event-db
  ::edit-job-seeking-status
  profile-update-interceptors
  (fn [db [status]]
    (assoc db :job-seeking-status status)))

(reg-event-db
  ::toggle-visa-status
  profile-update-interceptors
  (fn [db [status]]
    (update db :visa-status util/toggle status)))

(reg-event-db
  ::toggle-role-type
  profile-update-interceptors
  (fn [db [role-type]]
    (update db :role-types util/toggle role-type)))

(reg-event-db
  ::toggle-prefer-remote
  profile-update-interceptors
  (fn [db _]
    (update db :remote #(not %))))

(reg-event-db
  ::edit-visa-status-other
  profile-update-interceptors
  (fn [db [status]]
    (assoc db :visa-status-other status)))

(reg-event-db
  ::edit-salary-min
  profile-update-interceptors
  (fn [db [value]]
    (let [salary (if (empty? value)
                   nil
                   (->> value js/parseInt js/Math.abs))]
      (assoc-in db [:salary :min] salary))))

(reg-event-db
  ::edit-salary-currency
  profile-update-interceptors
  (fn [db [currency]]
    (assoc-in db [:salary :currency] currency)))

(reg-event-db
  ::edit-salary-time-period
  profile-update-interceptors
  (fn [db [time-period]]
    (assoc-in db [:salary :time-period] time-period)))

(reg-event-fx
  ::current-location-search-success
  profile-update-interceptors
  (fn [{db :db} [result]]
    {:db (assoc db :current-location-suggestions result)}))

(reg-event-fx
  ::location-search-failure
  (fn [_ _]
    {}))

(reg-event-db
  ::select-current-location-suggestion
  profile-update-interceptors
  (fn [db [item]]
    (assoc db
      :current-location item
      :current-location-text nil
      :current-location-suggestions nil)))

(reg-event-fx
  ::edit-current-location
  profile-update-interceptors
  (fn [{db :db} [loc]]
    {:db       (assoc db :current-location-text loc)
     :dispatch [::location-events/search {:query      loc
                                          :on-success [::current-location-search-success]
                                          :on-failure [::location-search-failure]}]}))

(reg-event-fx
  ::location-search-success
  profile-update-interceptors
  (fn [{db :db} [i result]]
    {:db (assoc-in db [:location-suggestions i] result)}))

(reg-event-fx
  ::edit-preferred-location
  profile-update-interceptors
  (fn [{db :db} [i loc]]
    {:db       (assoc-in db [:preferred-locations i] loc)
     :dispatch [::location-events/search {:query      loc
                                          :on-success [::location-search-success i]
                                          :on-failure [::location-search-failure]}]}))

(reg-event-db
  ::select-suggestion
  profile-update-interceptors
  (fn [db [i item]]
    (-> db
        (assoc-in [:preferred-locations i] item)
        (assoc-in [:location-suggestions i] nil))))

(reg-event-fx
  ::remove-preferred-location
  profile-update-interceptors
  (fn [{db :db} [i]]
    {:db (update db :preferred-locations #(vec (util/drop-ith i %)))}))

;; profile update ---------

(defn db->db-with-user-info [db user-info]
  (let [profile-sub-db (::profile/sub-db db)
        user-sub-db (::user/sub-db db)
        ;; push updated user info to profile sub-db
        profile-sub-db' (->> user-info
                             (keywords/namespace-map (namespace ::profile/sub-db))
                             (merge profile-sub-db))
        ;; push updated user info to user sub-db
        user-sub-db' (->> user-info
                          (keywords/namespace-map (namespace ::user/sub-db))
                          (merge user-sub-db))]
    (assoc db ::profile/sub-db profile-sub-db'
              ::user/sub-db user-sub-db')))

(reg-event-fx
  ::update-profile-failure
  (fn [{db :db} _]
    (let [initial-user-info (get-in db [::profile-update-private/sub-db :initial-values])]
      {:db (db->db-with-user-info db initial-user-info)
       :dispatch [:error/set-global "Failed to update your profile, please try again."]})))

(reg-event-db
  ::update-profile-success
  profile-update-interceptors
  (fn [db _]
    ;; indicator that form is not initialized
    (dissoc db :initial-values)))

(defn prepare-user-info [user-info]
  (let [other-visa-status-selected? (profile-update-private/other-visa-status-present? (:visa-status user-info))]
    (cond-> user-info
            :always util/remove-nil-blank-or-empty
            ;; in case user deselected "Other" option in visa status but put something as :visa-status-other
            (not other-visa-status-selected?) (dissoc :visa-status-other))))

(reg-event-fx
  ::update-profile
  db/default-interceptors
  (fn [{db :db} _]
    (let [profile-update-sub-db (::profile-update-private/sub-db db)
          user-info (-> profile-update-sub-db
                        (select-keys profile-keys)
                        prepare-user-info)
          errors? (->> profile-update-sub-db
                       profile-update-private/form->errors
                       empty?
                       not)]
      (if errors?
        {:db (assoc-in db [::profile-update-private/sub-db :submit-attempted?] true)}
        {:graphql  {:query      graphql/update-user-mutation--profile
                    :variables  {:update_user (keywords/transform-keys user-info)}
                    :on-success [::update-profile-success]
                    :on-failure [::update-profile-failure]}
         :dispatch [:error/close-global]
         :db       (-> db
                       (db->db-with-user-info user-info)
                       (update ::profile-update-private/sub-db assoc :editing-profile? false))}))))
