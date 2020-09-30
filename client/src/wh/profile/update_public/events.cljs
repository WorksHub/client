(ns wh.profile.update-public.events
  (:require [re-frame.core :refer [path reg-event-db reg-event-fx]]
            [wh.common.errors :as errors]
            [wh.common.graphql-queries :as graphql]
            [wh.common.keywords :as keywords]
            [wh.common.upload :as upload]
            [wh.components.forms.events :as form-events]
            [wh.db :as db]
            [wh.logged-in.profile.db :as profile]
            [wh.profile.update-public.db :as profile-update-public]
            [wh.user.db :as user]))

(def profile-update-interceptors
  (into db/default-interceptors [(path ::profile-update-public/sub-db)]))

(def profile-public-keys [:id :name :summary :other-urls :image-url])

(reg-event-db
  ::open-modal
  db/default-interceptors
  (fn [db _]
    (let [profile-sub-db (::profile/sub-db db)
          profile-update-sub-db (::profile-update-public/sub-db db)
          fields-to-copy (-> profile-sub-db
                             keywords/strip-ns-from-map-keys
                             (select-keys profile-public-keys))
          form-initialized? (:initial-values profile-update-sub-db)]
      (cond-> db
              :always
              (assoc-in [::profile-update-public/sub-db :editing-profile?] true)
              (not form-initialized?)
              (update ::profile-update-public/sub-db #(-> %
                                                          (merge fields-to-copy)
                                                          (assoc :initial-values fields-to-copy)))))))

(reg-event-db
  ::close-modal
  profile-update-interceptors
  (fn [db _]
    (assoc db :editing-profile? false)))

(reg-event-db
  ::edit-name
  profile-update-interceptors
  (fn [db [name]]
    (assoc db :name name)))

(reg-event-db
  ::edit-summary
  profile-update-interceptors
  (fn [db [name]]
    (assoc db :summary name)))

(reg-event-db
  ::edit-url
  profile-update-interceptors
  (form-events/multi-edit-fn :other-urls :url))

;; avatar upload -------

(reg-event-fx
  ::avatar-upload
  profile-update-interceptors
  upload/image-upload-fn)

(reg-event-db
  ::avatar-upload-start
  profile-update-interceptors
  (fn [db _]
    (assoc db :image-uploading? true)))

(reg-event-db
  ::avatar-upload-success
  profile-update-interceptors
  (fn [db [_ {:keys [url]}]]
    (assoc db
      :image-url url
      :image-uploading? false)))

(reg-event-fx
  ::avatar-upload-failure
  profile-update-interceptors
  (fn [{db :db} [resp]]
    {:db       (assoc db :image-uploading? false)
     :dispatch [:error/set-global(errors/image-upload-error-message (:status resp))]}))

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
    (let [initial-user-info (get-in db [::profile-update-public/sub-db :initial-values])]
      {:db (db->db-with-user-info db initial-user-info)
       :dispatch [:error/set-global "Failed to update your profile, please try again."]})))

(reg-event-db
  ::update-profile-success
  profile-update-interceptors
  (fn [db _]
    ;; indicator that form is not initialized
    (dissoc db :initial-values)))

(reg-event-fx
  ::update-profile
  db/default-interceptors
  (fn [{db :db} _]
    (let [profile-update-sub-db (::profile-update-public/sub-db db)
          user-info (select-keys profile-update-sub-db profile-public-keys)
          errors? (->> profile-update-sub-db
                       profile-update-public/form->errors
                       empty?
                       not)]
      (if errors?
        {:db (assoc-in db [::profile-update-public/sub-db :submit-attempted?] true)}
        {:graphql {:query      graphql/update-user-mutation--profile
                   :variables  {:update_user (keywords/transform-keys user-info)}
                   :on-success [::update-profile-success]
                   :on-failure [::update-profile-failure]}
         :db      (-> db
                      (db->db-with-user-info user-info)
                      (update ::profile-update-public/sub-db assoc :editing-profile? false))}))))
