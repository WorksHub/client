(ns wh.logged-in.dashboard.events
  (:require
    [cljs.reader :as reader]
    [goog.crypt.base64 :as base64]
    [re-frame.core :refer [dispatch path reg-event-db reg-event-fx]]
    [wh.common.cases :as cases]
    [wh.common.graphql-queries :as queries]
    [wh.common.job :as job]
    [wh.db :as db]
    [wh.graphql.jobs :as jobs]
    [wh.logged-in.dashboard.db :as dashboard]
    [wh.pages.core :refer [on-page-load] :as pages]
    [wh.user.db :as user])
  (:require-macros
    [wh.graphql-macros :refer [defquery]]))

(def dashboard-interceptors (into db/default-interceptors
                                  [(path ::dashboard/sub-db)]))

(reg-event-db
  ::initialize-db
  dashboard-interceptors
  (fn [db _]
    (merge db dashboard/default-db)))

(reg-event-fx
  ::fetch-recommended-jobs-success
  dashboard-interceptors
  (fn [{db :db} [{{:keys [jobs]} :data}]]
    {:db (assoc db
                ::dashboard/jobs (mapv job/translate-job jobs)
                ::dashboard/loading-recommended? false)}))

(reg-event-fx
  ::fetch-recommended-jobs
  dashboard-interceptors
  (fn [{db :db} _]
    {:db      (assoc db ::dashboard/loading-recommended? true)
     :graphql {:query      queries/recommended-jobs-for-user
               :variables  {:page_size   3
                            :page_number 1}
               :on-success [::fetch-recommended-jobs-success]}}))

(defquery dashboard-data-query
  {:venia/operation {:operation/type :query
                     :operation/name "dashboard"}
   :venia/queries   [[:blogs {:filter_type "recommended"
                              :page_size   3
                              :page_number 1}
                      [[:blogs [:id :title :feature :author
                                :formattedCreationDate :score
                                :readingTime :published :upvoteCount
                                [:tags :fragment/tagFields]]]]]
                     [:me [:onboardingMsgs]]
                     [:jobs {:filter_type "recommended"
                             :entity_type "user"
                             :page_number 1
                             :page_size   3}
                      :fragment/jobCardFields]]})

(reg-event-fx
  ::fetch-initial-data
  db/default-interceptors
  (fn [_ _]
    {:graphql {:query      dashboard-data-query
               :on-success [::fetch-initial-data-success]
               :on-failure [::fetch-initial-data-failure]}}))

(reg-event-fx
  ::fetch-initial-data-success
  db/default-interceptors
  (fn [{db :db} [{{:keys [blogs jobs me]} :data}]]
    {:db       (-> db
                   (update ::dashboard/sub-db merge
                           {::dashboard/blogs (mapv cases/->kebab-case (:blogs blogs))
                            ::dashboard/jobs (mapv job/translate-job jobs)})
                   (update ::user/sub-db merge
                           {::user/onboarding-msgs (set (:onboardingMsgs me))}))
     :dispatch-n (into [[::pages/unset-loader]]
                       (when-let [events (get-in db [:wh.db/query-params "events"])]
                         (-> events base64/decodeString reader/read-string)))}))

(reg-event-fx
  ::fetch-initial-data-failure
  dashboard-interceptors
  (fn [{db :db} [_]]
    {:db (assoc db ::dashboard/error :unavailable)
     :dispatch [::pages/unset-loader]}))

(defmethod on-page-load :homepage-dashboard [db]
  [[::fetch-initial-data]
   [::pages/set-loader]])
