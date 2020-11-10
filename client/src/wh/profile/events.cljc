(ns wh.profile.events
  (:require
    [re-frame.core :refer [dispatch path reg-event-db reg-event-fx]]
    [wh.admin.queries :as admin-queries]
    #?(:cljs [wh.common.graphql-queries :as queries])
    [wh.db :as db]
    [wh.graphql-cache :refer [reg-query] :as gql-cache]
    [wh.graphql.fragments :as _fragments]
    [wh.profile.db :as profile]
    #?(:cljs [wh.pages.core :as pages]))
  (#?(:clj :require :cljs :require-macros)
   [wh.graphql-macros :refer [defquery]]))

(def profile-interceptors (into db/default-interceptors
                                [(path ::profile/sub-db)]))

(defquery profile-query
  {:venia/operation {:operation/type :query
                     :operation/name "fetch_user"}
   :venia/variables [{:variable/name "id"
                      :variable/type :ID!}
                     {:variable/name "job_id"
                      :variable/type :ID}]
   :venia/queries   [[:user {:id :$id
                             :jobId :$job_id}
                      [[:skills [:name :rating
                                 [:tag :fragment/tagFields]]]
                       [:interests :fragment/tagFields]
                       [:otherUrls [:url]]
                       :imageUrl
                       :email
                       :name
                       :id
                       :summary
                       :percentile
                       :published
                       :created
                       :lastSeen
                       :updated
                       :hubspotProfileUrl
                       :visaStatus :visaStatusOther
                       [:currentLocation [:city :administrative :country :countryCode :subRegion :region :longitude :latitude]]
                       [:preferredLocations [:city :administrative :country :countryCode :subRegion :region :longitude :latitude]]
                       [:cv [:link [:file [:type :name :url]]]]
                       [:applied [:timestamp
                                  :state
                                  :note
                                  [:coverLetter [:link [:file [:url]]]]
                                  [:job [:id :slug :title [:company [:name]]]]]]
                       [:likes [:id :slug :title [:company [:name]]]]

                       [:approval [:status :source :time]]

                       [:contributionsCollection
                        [:totalCommitContributions
                         :totalRepositoriesWithContributedCommits
                         [:contributionCalendar
                          [[:weeks
                            [[:contributionDays
                              [:contributionCount
                               :date :weekday :color]]]]]]]]]]

                     [:blogs {:user_id :$id}
                      [[:blogs [:id :title :formattedCreationDate
                                :readingTime :upvoteCount :published]]]]

                     [:query_issues {:user_id :$id}
                      [[:issues [:id :title :level
                                 [:compensation [:amount :currency]]
                                 [:company [:id :name :logo :slug]]
                                 [:repo [:primary_language]]]]]]]})

(reg-query :profile profile-query)

(defn profile-query-description [db]
  (let [job-id (get-in db [:wh.db/query-params "job-id"])
        user-id (get-in db [:wh.db/page-params :id])]
    [:profile (cond-> {:id user-id}
                      job-id (merge {:job_id job-id}))]))

(reg-event-fx
  ::set-page-title
  db/default-interceptors
  (fn [{db :db} _]
    {:page-title {:page-name (some-> (apply gql-cache/result db (profile-query-description db)) :user :name)
                  :vertical  (:wh.db/vertical db)}}))

(reg-event-fx
  ::load-profile
  db/default-interceptors
  (fn [{db :db} _]
    {:dispatch (into [:graphql/query]
                     (conj (profile-query-description db)
                           {:on-complete [::set-page-title]
                            :on-success  [:wh.events/scroll-to-top]
                            :force true}))}))

(reg-event-fx
  ::set-approval-status-failure
  profile-interceptors
  (fn [{db :db} [[id status]]]
    {:db      (profile/unset-updating-status db)
     :dispatch [:error/set-global "Failed to set user status."
                [::set-approval-status id status]]}))

(reg-event-fx
  ::set-approval-status-success
  profile-interceptors
  (fn [{db :db} _]
    {:db      (profile/unset-updating-status db)
     :dispatch [::load-profile]}))

(reg-event-fx
  ::set-approval-status
  profile-interceptors
  (fn [{db :db} [id status]]
    {:db      (profile/set-updating-status db status)
     :graphql {:query      admin-queries/set-approval-status-mutation
               :variables  {:id id :status status}
               :on-success [::set-approval-status-success]
               :on-failure [::set-approval-status-failure [id status]]}}))

;; ----------------------------------------------------------

(reg-event-db
  ::open-user-info-modal
  db/default-interceptors
  (fn [db [{:keys [state] :as opts}]]
    (if (= (profile/application-state :get-in-touch) state)
      (profile/open-modal db)
      db)))

(reg-event-db
  ::close-user-info-modal
  db/default-interceptors
  (fn [db _]
    (profile/close-modal db)))

(reg-event-fx
  ::set-application-state-success
  profile-interceptors
  (fn [{db :db} [{:keys [state] :as opts}]]
    {:db (profile/finish-updating-application-state db)
     :dispatch-n [[::load-profile] [::open-user-info-modal opts]]}))

(reg-event-fx
  ::set-application-state-failure
  profile-interceptors
  (fn [{db :db} _]
    {:db (profile/finish-updating-application-state db)
     :dispatch [:error/set-global "Something went wrong while we tried to change this application's state, please retry"]}))

#?(:cljs
   (reg-event-fx
     ::set-application-state
     profile-interceptors
     (fn [{db :db} [{:keys [user application state] :as opts}]]
       {:db      (profile/start-updating-application-state db)
        :graphql {:query      queries/set-application-state-mutation
                  :variables  {:input {:user_id (:id user)
                                       :job_ids [(get-in application [:job :id])]
                                       :action  state}}
                  :on-success [::set-application-state-success opts]
                  :on-failure [::set-application-state-failure]}})))

#?(:cljs
   (defmethod pages/on-page-load :user [_]
     [[:wh.pages.core/unset-loader]
      [::load-profile]]))
