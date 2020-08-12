(ns wh.register-new.events
  (:require
    [re-frame.core :refer [reg-event-db reg-event-fx]]
    [wh.db :as db]
    [wh.login.db :as login]
    [wh.register-new.db :as register]
    [wh.util :as util])
  (:require-macros
    [wh.graphql-macros :refer [defquery]]))

(reg-event-db
  ::initialize-db
  db/default-interceptors
  (fn [db _]
    (register/initialize-sub-db db)))

(reg-event-db
  ::set-name
  db/default-interceptors
  (fn [db [name]]
    (register/set-name db name)))

(reg-event-db
  ::set-email
  db/default-interceptors
  (fn [db [email]]
    (register/set-email db email)))

(defquery create-user-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "create_user"}
   :venia/variables [{:variable/name "create_user"
                      :variable/type :CreateUserInput!}
                     {:variable/name "force_unapproved"
                      :variable/type :Boolean}]
   :venia/queries   [[:create_user {:create_user :$create_user
                                    :force_unapproved :$force_unapproved}
                      [:id [:approval [:status]] :email :name :consented :imageUrl :type
                       [:skills [:name]]
                       [:preferredLocations
                        [:city :administrative :country :countryCode :subRegion :region :longitude :latitude]]]]]})

(reg-event-db
  ::create-user-failure
  db/default-interceptors
  (fn [db [response]]
    (-> db
        register/unset-submitting-flag
        (register/set-error response))))

(reg-event-fx
  ::create-user-success
  db/default-interceptors
  (fn [{db :db} [{data :data}]]
    (let [user (:create_user data)]
      {:db         (-> db
                       register/unset-submitting-flag
                       (register/db->db-with-user user))
       :dispatch-n (into [[:register/track-account-created
                           (if (register/stackoverflow-signup? db)
                             {:source :stackoverflow :id (register/db->stackoverflow-account-id db)}
                             {:source :email :email (:email user)})]]
                         (login/redirect-post-login-or-registration {}))})))

(reg-event-fx
  ::create-user
  db/default-interceptors
  (fn [{:keys [db]} _]
    {:db      (-> db
                  register/set-submitting-flag
                  register/unset-error)
     :graphql {:query      create-user-mutation
               :variables  {:create_user (util/remove-nils
                                           {:name                     (-> db register/db->sub-db register/->name)
                                            :email                    (-> db register/db->sub-db register/->email)
                                            :consented                (js/Date.)
                                            :stackoverflowAccessToken (register/db->stackoverflow-access-token db)})
                            :force_unapproved true}
               :on-success [::create-user-success]
               :on-failure [::create-user-failure]}}))