(ns wh.company.candidate.events
  (:require
    [re-frame.core :refer [path reg-event-db reg-event-fx]]
    [wh.common.graphql-queries :as queries]
    [wh.company.candidate.db :as candidate]
    [wh.db :as db]
    [wh.logged-in.profile.db :as profile]
    [wh.pages.core :as pages :refer [on-page-load]]
    [wh.user.db :as user]
    [wh.util :as util])
  (:require-macros
    [wh.graphql-macros :refer [defquery]]))

(def candidate-interceptors (into db/default-interceptors
                                  [(path ::candidate/sub-db)]))

(defquery set-user-platform-approval-status-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "update_user_approval_status"}
   :venia/variables [{:variable/name "id"
                      :variable/type :ID!}
                     {:variable/name "status"
                      :variable/type :String!}]
   :venia/queries [[:update_user_approval_status {:id :$id :status :$status}
                    [:id]]]})

(defn candidate-id [db]
  (get-in db [::db/page-params :id]))

(defn candidate-query [id admin?]
  (let [fields (concat [[:skills [:name :rating]]
                        [:otherUrls [:url]]
                        :email
                        :id
                        :imageUrl
                        :name :summary
                        :visaStatus :visaStatusOther
                        [:blogs [:id :title :upvoteCount]]
                        [:cv [:link
                              [:file [:type :name :url]]]]
                        :remote
                        [:currentLocation [:city :administrative :country :countryCode :subRegion :region :longitude :latitude]]
                        [:preferredLocations [:city :administrative :country :countryCode :subRegion :region :longitude :latitude]]]
                       (if admin?
                         [[:companyPerks [:name]]
                          [:approval [:status :source :time]]
                          [:likes [:id :title [:company [:name]]]]
                          [:applied [[:job [:id :title [:company [:name]]]]]]
                          :jobSeekingStatus :roleTypes
                          :hubspotProfileUrl
                          :type
                          [:company [:id :name]]
                          [:salary [:currency :min :timePeriod]]]
                         ;;
                         [[:applied [:timestamp :state :note [:job [:id :title]]]]]))]
    {:venia/queries [[:user {:id id} fields]]}))

(reg-event-fx
  ::load-candidate-success
  db/default-interceptors
  (fn [{db :db} [{:keys [data]}]]
    (let [user (user/graphql-user->db (:user data))]
      {:dispatch-n [[::pages/unset-loader]]
       :db (-> db
               (assoc-in [::candidate/sub-db ::candidate/data] user)
               (update ::profile/sub-db merge (profile/->sub-db user)))})))

(reg-event-fx
  ::load-candidate-failure
  candidate-interceptors
  (fn [{db :db} [resp]]
    {:db       (assoc db ::candidate/error (util/gql-errors->error-key resp))
     :dispatch [::pages/unset-loader]}))

(reg-event-fx
  ::load-candidate
  db/default-interceptors
  (fn [{db :db} _]
    (let [id (get-in db [::db/page-params :id])]
      {:dispatch [::pages/set-loader]
       :graphql {:query (candidate-query id (user/admin? db))
                 :on-success [::load-candidate-success]
                 :on-failure [::load-candidate-failure]}})))

(reg-event-fx
  :candidate/set-approval-status
  candidate-interceptors
  (fn [{db :db} [id email status]]
    {:db      (assoc-in db [::candidate/data :updating] status)
     :graphql {:query      set-user-platform-approval-status-mutation
               :variables  {:id id :status status}
               :timeout    30000
               :on-success [::load-candidate]
               :on-failure [:error/set-global (str "Failed to set status to " status " for " email)
                            [:candidate/set-approval-status id email status]]}}))

(reg-event-db
  ::initialize-db
  db/default-interceptors
  (fn [db _]
    (assoc db ::candidate/sub-db candidate/default-db)))

(defn redirect
  [db]
  [:login/set-redirect :candidate :params {:id (get-in db [::db/page-params :id])}])

(reg-event-fx
  ::go-to-magic-form
  db/default-interceptors
  (fn [{db :db}]
    {:dispatch-n [(redirect db)
                  [:wh.events/nav :login :params {:step :email}]
                  [:wh.login.events/show-magic-form]]}))

(reg-event-fx
  ::set-application-state
  db/default-interceptors
  (fn [{db :db} [job-id-or-ids state]]
    {:graphql {:query queries/set-application-state-mutation
               :variables {:input {:user_id (candidate-id db)
                                   :job_ids (util/->vec job-id-or-ids)
                                   :action state}}
               :on-success [::set-application-state-success job-id-or-ids]
               :on-failure [::set-application-state-failure job-id-or-ids state]}
     :db (assoc-in db [::candidate/sub-db ::candidate/updating?] true)}))

(defn update-state-fn
  [job-id-or-ids state]
  (partial mapv #(if (or (= job-id-or-ids (-> % :job :id))
                         (contains? (set job-id-or-ids) (-> % :job :id)))
                   (assoc % :state state)
                   %)))

(reg-event-fx
  ::set-application-state-success
  db/default-interceptors
  (fn [{db :db} [job-id-or-ids resp]]
    (let [state (-> resp :data :set_application_state :states first (subs 1))]
      (merge {:db (-> db
                      (assoc-in  [::candidate/sub-db ::candidate/updating?] false)
                      (update-in [::candidate/sub-db ::candidate/data :applied] (update-state-fn job-id-or-ids state))
                      (update-in [::profile/sub-db ::profile/applied] (update-state-fn job-id-or-ids state)))}
             (when (and job-id-or-ids (= state "get_in_touch"))
               {:dispatch [::show-get-in-touch-overlay (when-not (sequential? job-id-or-ids) job-id-or-ids)]})))))

(reg-event-fx
  ::set-application-state-failure
  db/default-interceptors
  (fn [{db :db} [job-id-or-ids state _resp]]
    {:db (assoc-in db [::candidate/sub-db ::candidate/updating?] false)
     :dispatch-n [[:error/set-global "Something went wrong while we tried to change this application's state 😢"
                   [::set-application-state job-id-or-ids state]]]}))

(reg-event-db
  ::show-get-in-touch-overlay
  candidate-interceptors
  (fn [db [job-id]]
    (assoc db ::candidate/get-in-touch-overlay job-id)))

(reg-event-db
  ::hide-get-in-touch-overlay
  candidate-interceptors
  (fn [db _]
    (dissoc db ::candidate/get-in-touch-overlay)))

(reg-event-db
  ::show-job-selection-overlay
  candidate-interceptors
  (fn [db [state]]
    (assoc db ::candidate/job-selection-overlay-state state)))

(reg-event-fx
  ::hide-job-selection-overlay
  candidate-interceptors
  (fn [{db :db} [dismissed?]]
    (merge {:db (dissoc db
                        ::candidate/job-selection-overlay-state
                        ::candidate/job-selection-overlay-job-selections)}
           (when-not dismissed?
             (when-let [job-ids (not-empty (::candidate/job-selection-overlay-job-selections db))]
               {:dispatch [::set-application-state
                           job-ids
                           (::candidate/job-selection-overlay-state db)]})))))

(reg-event-db
  ::toggle-job-for-application-state
  candidate-interceptors
  (fn [db [job-id]]
    (update db ::candidate/job-selection-overlay-job-selections util/toggle job-id)))

(reg-event-fx
  ::send-to-payment-flow
  (fn [_ [_ candidate-id]]
    {:navigate [:payment-setup
                :params {:step :select-package}
                :query-params {:action :applications
                               :candidate candidate-id}]}))

(defmethod on-page-load :candidate [db]
  (if (db/logged-in? db)
    (if (and (user/company? db) (not (user/has-permission? db :can_see_applications)))
      [[::send-to-payment-flow (candidate-id db)]]
      [[::initialize-db]
       [::load-candidate]])
    []))
