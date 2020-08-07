(ns wh.login.stackoverflow-callback.events
  (:require
    [re-frame.core :refer [reg-event-fx reg-event-db inject-cofx]]
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
  ::request-fail
  db/default-interceptors
  (fn [{db :db} [{:keys [errors] :as _resp}]]
    {:dispatch [::pages/unset-loader]
     :db       (-> db
                   (assoc-in [::stackoverflow-callback/sub-db :error] (first errors))
                   (assoc-in [::stackoverflow-callback/sub-db :callback-status] :fail)
                   (assoc ::db/page :stackoverflow-callback))}))

;; TODO: CH4172: kill it after after fixing user type in leona
(defquery user-details
  {:venia/operation {:operation/type :query
                     :operation/name "Me"}
   :venia/queries   [[:me
                      [:id :name :visaStatus :visaStatusOther [:approval [:status]]
                       :type :email :new :githubId :consented [:skills [:name]]
                       [:githubInfo [:name [:skills [:name]]]]
                       [:twitterInfo [:id]]
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
          db (-> db
                 (update ::user/sub-db merge (prepare-user user))
                 (assoc-in [::stackoverflow-callback/sub-db :callback-status] :success))
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
                   :on-failure [::request-fail]}}))))

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
                           :account_id
                           :tags]]]]
                       [:user
                        [:id :image_url]]]]]})

(reg-event-fx
  ::stackoverflow-callback
  [(inject-cofx :persistent-state)]
  (fn [cofx [_ code]]
    (let [db (if (empty? (:persistent-state cofx))
               (:db cofx)
               (merge (select-keys (:db cofx) [::db/page-mapping])
                      (:persistent-state cofx)))]
      (if (= (get-in db [::stackoverflow-callback/sub-db :callback-status]) :sent)
        {:db db}
        {:db            (assoc-in db [::stackoverflow-callback/sub-db :callback-status] :sent)
         :graphql       {:query      stackoverflow-auth-mutation
                         :variables  {:stackoverflowCode code}
                         :on-success [::stackoverflow-auth-success]
                         :on-failure [::request-fail]}
         :persist-state {}}))))

(reg-event-db
  ::init-db
  db/default-interceptors
  (fn [db _]
    (update db ::stackoverflow-callback/sub-db merge stackoverflow-callback/default-db)))

(defmethod on-page-load :stackoverflow-callback [db]
  (let [qp (::db/query-params db)]
    [[::init-db]
     [::stackoverflow-callback (qp "code")]]))
