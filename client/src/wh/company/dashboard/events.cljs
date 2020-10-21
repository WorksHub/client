(ns wh.company.dashboard.events
  (:require [cljs-time.core :as t]
            [cljs-time.format :as tf]
            [cljs.reader :as reader]
            [clojure.string :as str]
            [goog.crypt.base64 :as base64]
            [re-frame.core :refer [path reg-event-db reg-event-fx]]
            [wh.common.cases :as cases]
            [wh.common.job :as common-job]
            [wh.common.keywords :as keywords]
            [wh.common.user :as user-common]
            [wh.company.dashboard.db :as sub-db]
            [wh.db :as db]
            [wh.graphql.company :refer [update-company-mutation]]
            [wh.job.events :refer [process-publish-role-intention]]
            [wh.pages.core :as pages :refer [on-page-load]]
            [wh.user.db :as user]
            [wh.util :as util])
  (:require-macros [wh.graphql-macros :refer [defquery]]))

(def company-interceptors (into db/default-interceptors
                                [(path ::sub-db/sub-db)]))

(defn- date->str [d]
  (tf/unparse (tf/formatters :date) d))

(defn- str->date-time [s]
  (when-not (str/blank? s)
    (tf/parse (tf/formatters :date-time) s)))

(defquery dashboard-query
  {:venia/operation {:operation/type :query
                     :operation/name "dashboard_population"}
   ;; company_id and company_jobs_id are actually the same thing.
   ;; Repetition is caused by different GraphQL fragments requiring
   ;; different variables types.
   ;; TODO: unify those types into one
   :venia/variables [{:variable/name "company_id" :variable/type :ID}
                     {:variable/name "company_jobs_id" :variable/type :String!}
                     {:variable/name "end_date" :variable/type :String!}]
   :venia/queries   [[:company {:id :$company_id}
                      [:id :name :descriptionHtml :logo :package :permissions :disabled :slug :profileEnabled

                       ;; ----------
                       ;; required for profile completion percentage
                       :size :foundedYear :howWeWork
                       [:tags [:id :type :label :slug :subtype]]
                       [:techScales [:testing :ops :timeToDeploy]]
                       [:locations [:city]]
                       [:videos [:youtubeId]]
                       [:images [:url]]
                       [:blogs {:pageSize 1 :pageNumber 1}
                        [[:blogs
                          [:id]]]]
                       ;; ----------
                       [:pendingOffer [:recurringFee]]
                       [:payment [:billingPeriod]]]]
                     [:jobs {:filter_type "all"
                             :company_id  :$company_jobs_id
                             :page_size   100
                             :page_number 1}
                      [:id :slug :title [:tags :fragment/tagFields] :published :firstPublished :creationDate :verticals
                       [:location [:city :state :country :countryCode]]
                       [:stats [:applications :views :likes]]
                       :matchingUsers]]
                     [:me [:onboardingMsgs]]
                     [:job_analytics {:company_id  :$company_id
                                      :end_date    :$end_date
                                      :granularity 0
                                      :num_periods 0}
                      [:granularity
                       [:applications [:date :count]]
                       [:views [:date :count]]
                       [:likes [:date :count]]]]
                     [:activity {:company_id :$company_id}
                      [:type :jobId :jobTitle :jobSlug :userId :userName :count :timestamp]]]})

(defmethod on-page-load :company-dashboard [db]
  [[::initialize-db]
   [::fetch-company]])

(reg-event-db
  ::initialize-db
  company-interceptors
  (fn [db _]
    (sub-db/initial-db db)))

;; How many items to display initially in activity? We normally
;; want 15, but it can be too many when a company has one job only.
;; We don't want to impose any max-height on the activity div,
;; because that would tamper with its ability to display more
;; items when user explicitly asks for that. Ideally, we would
;; be able to ask the browser "how tall it would be with this
;; many elements?", but I don't know any way to do that short of
;; actually rendering the elements, which would be hacky and
;; introduce flickering.
;; So we define conservative estimates on the heights of
;; individual activity items and check how many will fit,
;; based on the number of jobs.

(def heights
  {"codi" 56, "views" 74, "application" 92, "match" 128, "like" 74})

(defn initial-activity-count
  [num-jobs activity]
  (let [item-heights (map (comp heights :type) activity)
        cumulative-heights (reductions + item-heights)
        num-job-rows (int (/ (+ num-jobs 2) 2))
        max-height (- (* num-job-rows 413) 36)]
    (min 15 (count (take-while #(< % max-height) cumulative-heights)))))

(defn company-id
  [db]
  (cond
    (user-common/company? db) (get-in db [::user/sub-db ::user/company-id])
    (user-common/admin? db) (get-in db [::db/page-params :id])))

(reg-event-db
  ::fetch-permissions-success
  (fn [db [_ {{:keys [me]} :data}]]
    (let [permissions (get-in me [:company :permissions])]
      (assoc-in db [::user/sub-db ::user/company :permissions] permissions))))

(reg-event-fx
  ::fetch-permissions
  db/default-interceptors
  (fn [{db :db} _]
    (let [id (user-common/user-id db)]
      {:graphql {:query      user-common/permissions-query
                 :variables  {:user_id id}
                 :on-success [::fetch-permissions-success]}})))

(reg-event-fx
  ::fetch-company
  db/default-interceptors
  (fn [{db :db} _]
    (let [id (company-id db)]
      {:graphql {:query      dashboard-query
                 :variables  {:company_id      id
                              :company_jobs_id id
                              :end_date        (-> (t/now) date->str)}
                 :on-success [::fetch-company-success]
                 :on-failure [::fetch-company-failure]}})))

(reg-event-fx
  ::show-more
  db/default-interceptors
  (fn [{db :db} _]
    {:db (update-in db [::sub-db/sub-db ::sub-db/activity-items-count] + 15)
     :analytics/track ["Show More Activities Clicked" {:company-id (company-id db)}]}))

(defn translate-job [job]
  (-> job
      common-job/translate-job
      (assoc :partial-view-data
             (when-let [created (str->date-time (:creationDate job))]
               (t/before? created (t/date-time 2018 7 18))))
      (update :first-published
              #(when-not (str/blank? %)
                 (subs % 0 10)))))

(reg-event-fx
  ::fetch-company-success
  db/default-interceptors
  (fn [{db :db} [resp]]
    {:db (let [company (-> resp
                           (get-in [:data :company])
                           (cases/->kebab-case)
                           (update :package keyword)
                           (update :size keyword)
                           (update :permissions #(set (map keyword %)))
                           (update :tags #(map (fn [tag] (-> tag
                                                            (update :type keyword)
                                                            (assoc :weight 0.0))) %)))
               company-db (as-> company x
                                (keywords/namespace-map (namespace `::sub-db/x) x)
                                (assoc x ::sub-db/stats (get-in resp [:data :job_analytics]))
                                (assoc x ::sub-db/jobs (mapv translate-job (get-in resp [:data :jobs])))
                                (assoc x ::sub-db/activity (->> (get-in resp [:data :activity])
                                                                (map cases/->kebab-case)
                                                                (mapv #(update % :timestamp str->date-time))))
                                (assoc x ::sub-db/activity-items-count
                                       (initial-activity-count (count (::sub-db/jobs x))
                                                               (::sub-db/activity x))))]
           (-> db
               (assoc :wh.company.dashboard.db/sub-db company-db)
               (util/update-in* [:wh.user.db/sub-db :wh.user.db/company]
                                (if (user-common/company? db)
                                  #(merge % company)
                                  identity))
               (assoc-in [:wh.user.db/sub-db :wh.user.db/onboarding-msgs] (set (get-in resp [:data :me :onboardingMsgs])))))
     :dispatch-n (or (some-> (get-in db [:wh.db/query-params "events"]) base64/decodeString reader/read-string) [])}))

(reg-event-db
  ::fetch-company-failure
  company-interceptors
  (fn [db data]
    (assoc db ::sub-db/error :failed-to-fetch-company)))

(reg-event-fx
  ::scroll-to-job
  db/default-interceptors
  (fn [_ [job-id]]
    {:scroll-into-view job-id}))

(reg-event-fx
  ::close-publish-celebration
  company-interceptors
  (fn [{db :db} [job-id]]
    {:db (-> db
             (update ::sub-db/publish-celebrations (comp #(disj % job-id) set))
             (update ::sub-db/jobs #(map (fn [job]
                                           (if (= job-id (:id job))
                                             (assoc job :published true)
                                             job)) %)))
     :dispatch-debounce {:id       :scroll-to-job-after-publish
                         :dispatch [::scroll-to-job (str "dashboard__job-card__" job-id)]
                         :timeout  300}}))

(reg-event-fx
  ::publish-job-success
  company-interceptors
  (fn [{db :db} [job-id]]
    {:db (-> db
             (update ::sub-db/publishing-jobs (comp #(disj % job-id) set))
             (update ::sub-db/publish-celebrations (comp #(conj % job-id) set)))
     :dispatch [::fetch-permissions]}))

(reg-event-db
  ::publish-job-failure
  company-interceptors
  (fn [db [job-id]]
    (update db ::sub-db/publishing-jobs (comp #(disj % job-id) set))))

(reg-event-fx
  ::publish-role
  db/default-interceptors
  (fn [{db :db} [job-id]]
    (let [perms         (get-in db [::sub-db/sub-db ::sub-db/permissions])
          pending-offer (get-in db [::sub-db/sub-db ::sub-db/pending-offer])]
      (process-publish-role-intention
        {:db             db
         :job-id         job-id
         :permissions    perms
         :pending-offer  pending-offer
         :publish-events {:success [::publish-job-success job-id]
                          :failure [::publish-job-failure job-id]
                          :retry   [::publish-role job-id]}
         :on-publish     (fn [db]
                           (update-in db [::sub-db/sub-db ::sub-db/publishing-jobs]
                                      (comp #(conj % job-id) set)))}))))

(reg-event-fx
  ::update-company
  company-interceptors
  (fn [{db :db} [company]]
    (let [c (keywords/namespace-map (namespace `::sub-db/x) company)]
      {:db (merge db c)})))

(reg-event-fx
  ::add-company-onboarding-msg
  db/default-interceptors
  (fn [{db :db} [msg]]
    (let [new-wms (set (conj (get-in db [:wh.user.db/sub-db ::wh.user.db/company :onboarding-msgs]) msg))]
      {:graphql {:query update-company-mutation
                 :variables {:update_company
                             {:id (company-id db)
                              :onboardingMsgs new-wms}}
                 :on-success [::add-company-onboarding-msg-success new-wms]}})))

(reg-event-db
  ::add-company-onboarding-msg-success
  db/default-interceptors
  (fn [db [new-wms]]
    (assoc-in db [:wh.user.db/sub-db ::wh.user.db/company :onboarding-msgs] new-wms)))
