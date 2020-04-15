(ns wh.login.stackoverflow-callback.events
  (:require
    [re-frame.core :refer [reg-event-fx inject-cofx]]
    [wh.common.fx.persistent-state]
    [wh.db :as db]
    [wh.login.db :as login]
    [wh.login.stackoverflow-callback.db :as stackoverflow-callback]
    [wh.pages.core :as pages :refer [on-page-load]]
    [wh.user.db :as user]
    [wh.util :as util])
  (:require-macros
    [wh.graphql-macros :refer [defquery]]))

(defn complete-login [db base-dispatch]
  {:db         db
   :analytics/identify db
   :dispatch-n (into base-dispatch (login/redirect-post-login-or-registration db))})

;; TODO: do we have function for this?
(defn- initialize-associated-jobs
  [{:keys [likes applied] :as user}]
  (-> user
      (assoc :liked-jobs (set (map :id likes))
             :applied-jobs (set (map :jobId applied)))
      (dissoc :likes :applied)))

(defn prepare-user [user]
  (-> user
      initialize-associated-jobs
      util/remove-nils
      user/translate-user))

(reg-event-fx
  ::user-details-success
  db/default-interceptors
  (fn [{db :db} [resp]]
    (let [user (get-in resp [:data :me])
          db (update db ::user/sub-db #(merge % (prepare-user user)))]
      (complete-login db [[::pages/unset-loader]]))))

(reg-event-fx
  ::user-details-fail
  db/default-interceptors
  (fn [{db :db} [_]]
    {:dispatch [::pages/unset-loader]
     :db       (-> db
                   (assoc-in [::stackoverflow-callback/sub-db
                              ::stackoverflow-callback/error?] true)
                   (assoc ::db/page :stackoverflow-callback))}))

;; TODO: kill it after after fixing user type in leona
(defquery user-details
  {:venia/operation {:operation/type :query
                     :operation/name "Me"}
   :venia/queries   [[:me
                      [:id :name :visaStatus :visaStatusOther [:approval [:status]] :type :email :new :githubId
                       :consented
                       [:githubInfo [:name [:skills [:name]]]]
                       [:cv [:link
                             [:file [:type :name :url]]]]
                       [:salary [:min :currency]]
                       [:likes [:id]]
                       [:applied [:jobId]]
                       [:preferredLocations [:city :administrative :country :countryCode :subRegion :region :longitude :latitude]]]]]})

(reg-event-fx
  ::stackoverflow-auth-success
  db/default-interceptors
  (fn [{db :db} [resp]]
    (let [resp-data (get-in resp [:data :stackoverflow_auth])
          user (or (:user_info_stackoverflow resp-data)
                   (:user resp-data))
          account-id (get-in user [:stackoverflow_info :account_id])
          new (:new user)
          db (update db ::user/sub-db #(merge % (prepare-user user)))
          base-dispatch (cond-> [[::pages/unset-loader]]
                                new (conj [:register/track-account-created {:source :stackoverflow :id account-id}])
                                (and new (:register/track-context db)) (conj [:register/track-start (:register/track-context db)]))]
      (if (:new user)
        {:db db
         :dispatch-n base-dispatch
         :navigate [:register :params {:step :email}]}
        {:db      db
         :graphql {:query      user-details
                   :on-success [::user-details-success]
                   :on-failure [::user-details-fail]}}))))

(reg-event-fx
  ::stackoverflow-auth-fail
  db/default-interceptors
  (fn [{db :db} [_]]
    {:dispatch [::pages/unset-loader]
     :db       (-> db
                   (assoc-in [::stackoverflow-callback/sub-db
                              ::stackoverflow-callback/error?] true)
                   (assoc ::db/page :stackoverflow-callback))}))

(defquery stackoverflow-auth-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "stackoverflow_auth"}
   :venia/variables [{:variable/name "stackoverflowCode"
                      :variable/type :String!}]
   :venia/queries   [[:stackoverflow_auth {:stackoverflow_code :$stackoverflowCode}
                      [[:user_info_stackoverflow
                        [:name
                         :new
                         [:stackoverflow_info
                          [:access_token
                           :account_id]]]]
                       [:user
                        [:id]]]]]})

(reg-event-fx
  ::stackoverflow-callback
  [(inject-cofx :persistent-state)]
  (fn [cofx [_ code]]
    (let [db (if (empty? (:persistent-state cofx))
               (:db cofx)
               (merge (select-keys (:db cofx) [::db/page-mapping])
                      (:persistent-state cofx)))]
      {:db            db
       :graphql       {:query      stackoverflow-auth-mutation
                       :variables  {:stackoverflowCode code}
                       :on-success [::stackoverflow-auth-success]
                       :on-failure [::stackoverflow-auth-fail]}
       :persist-state {}})))

(defmethod on-page-load :stackoverflow-callback [db]
  (let [qp (::db/query-params db)]
    [[::stackoverflow-callback (qp "code")]]))
