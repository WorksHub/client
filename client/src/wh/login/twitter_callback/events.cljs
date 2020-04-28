(ns wh.login.twitter-callback.events
  (:require
    [re-frame.core :refer [reg-event-fx inject-cofx reg-event-db]]
    [wh.common.fx.persistent-state]
    [wh.db :as db]
    [wh.login.db :as login]
    [wh.login.twitter-callback.db :as twitter-callback]
    [wh.pages.core :as pages :refer [on-page-load]]
    [wh.user.db :as user]
    [wh.util :as util])
  (:require-macros
    [wh.graphql-macros :refer [defquery]]))

(defn complete-login [db base-dispatch]
  {:db         db
   :analytics/identify db
   :dispatch-n (into base-dispatch (login/redirect-post-login-or-registration db))})

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
  (fn [{db :db} [_]]
    {:dispatch [::pages/unset-loader]
     :db       (-> db
                   (assoc-in [::twitter-callback/sub-db :error?] true)
                   (assoc-in [::twitter-callback/sub-db :callback-status] :fail)
                   (assoc ::db/page :twitter-callback))}))

;; TODO: CH4172: kill it after after fixing user type in leona
(defquery user-details
  {:venia/operation {:operation/type :query
                     :operation/name "Me"}
   :venia/queries   [[:me
                      [:id :name :visaStatus :visaStatusOther [:approval [:status]] :type :email :new :githubId
                       :consented
                       [:githubInfo [:name [:skills [:name]]]]
                       [:twitterInfo [:id]]
                       [:cv [:link
                             [:file [:type :name :url]]]]
                       [:salary [:min :currency]]
                       [:likes [:id]]
                       [:applied [:jobId]]
                       [:preferredLocations [:city :administrative :country :countryCode :subRegion :region :longitude :latitude]]]]]})

(reg-event-fx
  ::twitter-auth-success
  db/default-interceptors
  (fn [{db :db} [resp]]
    (let [user (get-in resp [:data :twitter_auth :user])
          account-id (get-in user [:twitter_info :id])
          new (:new user)
          db (-> db
                 (update ::user/sub-db merge (prepare-user user))
                 (assoc-in [::twitter-callback/sub-db :callback-status] :success))
          base-dispatch (cond-> []
                                new (conj [:register/track-account-created {:source :twitter :id account-id}])
                                (and new (:register/track-context db)) (conj [:register/track-start (:register/track-context db)]))]
      (if (:new user)
        {:db db
         :dispatch-n base-dispatch
         :navigate [:register :params {:step :email}]}
        {:db      db
         :graphql {:query      user-details
                   :on-success [::user-details-success]
                   :on-failure [::request-fail]}}))))

(defquery twitter-auth-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "twitter_auth"}
   :venia/variables [{:variable/name "token"
                      :variable/type :String!}
                     {:variable/name "verifier"
                      :variable/type :String!}]
   :venia/queries   [[:twitter_auth {:token :$token
                                     :verifier :$verifier}
                      [[:user
                        [:id
                         :name
                         :new
                         :email
                         [:twitter_info
                          [:id]]]]]]]})

(reg-event-fx
  ::twitter-callback
  [(inject-cofx :persistent-state)]
  (fn [cofx [_ [token verifier]]]
    (let [db (if (empty? (:persistent-state cofx))
               (:db cofx)
               (merge (select-keys (:db cofx) [::db/page-mapping])
                      (:persistent-state cofx)))]
      (if (= (get-in db [::twitter-callback/sub-db :callback-status]) :sent)
        {:db db}
        {:db            (assoc-in db [::twitter-callback/sub-db :callback-status] :sent)
         :graphql       {:query      twitter-auth-mutation
                         :variables  {:token    token
                                      :verifier verifier}
                         :on-success [::twitter-auth-success]
                         :on-failure [::request-fail]}
         :dispatch      [::pages/set-loader]
         :persist-state {}}))))

(reg-event-db
  ::init-db
  db/default-interceptors
  (fn [db _]
    (update db ::twitter-callback/sub-db merge twitter-callback/default-db)))

(defmethod on-page-load :twitter-callback [db]
  (let [qp (::db/query-params db)]
    [[::init-db]
     [::twitter-callback [(qp "oauth_token")
                          (qp "oauth_verifier")]]]))

