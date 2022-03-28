(ns wh.company.applications.events
  (:require [cljs-time.format :as tf]
            [clojure.string :as str]
            [re-frame.core :refer [path reg-event-db reg-event-fx]]
            [wh.common.cases :as cases]
            [wh.common.graphql-queries :as queries]
            [wh.common.job :refer [format-job-location]]
            [wh.common.url :as url]
            [wh.common.user :as user-common]
            [wh.company.applications.db :as sub-db]
            [wh.company.applications.subs :refer [action->state]]
            [wh.db :as db]
            [wh.graphql.company :refer [all-company-jobs-query-fragment]]
            [wh.pages.core :as pages :refer [on-page-load]]
            [wh.user.db :as user]
            [wh.util :as util]))

(def company-interceptors (into db/default-interceptors
                                [(path ::sub-db/sub-db)]))

(defn applications-query-args
  [{:keys [job-id company-id states page-number page-size]}]
  (merge {:page_size (or page-size sub-db/apps-page-size)
          :page_number page-number}
         (when company-id {:company_id company-id})
         (when (not-empty job-id) {:job_id job-id})
         (when (not-empty states) {:states states})))

(defn latest-applied-company-jobs-query
  [company-id page-number]
  {:venia/queries [[:latest_applied_jobs {:company_id company-id
                                          :page_size sub-db/latest-applied-jobs-page-size
                                          :page_number page-number}
                    [[:jobs [:id :slug :title :firstPublished
                             [:tags [:id :type :label :slug :subtype]]
                             [:location [:city :state :country :countryCode]]
                             [:stats [:applications :views :likes]]]]
                     [:pagination [:total]]]]]})

(def base-application-fields
  [:state :timestamp :score :userId :jobId :note :conversationId
   [:conversation [:id]]
   [:coverLetter [:link [:file [:url :name]]]]
   [:user [:name :email
           [:cv [:link [:file [:url :name]]]]
           [:otherUrls [:url :title]]
           [:currentLocation [:city :state :country :countryCode]]
           [:preferredLocations [:city :state :country :countryCode]]
           [:skills [:name]]]]])

(defn applications-query [{:keys [company-id page-number] :as args}]
  {:venia/queries (concat [[:applications
                            (applications-query-args args)
                            base-application-fields]]
                          (when (and (= 1 page-number) company-id)
                            [[:company {:id company-id} [:logo :name]]
                             (all-company-jobs-query-fragment company-id sub-db/jobs-page-size 1)]))})

(defn admin-applications-query
  [job-id manager-email states page-number]
  (let [app-fields (conj base-application-fields
                         [:job [:title [:company [:name]]]])
        vars (applications-query-args {:job-id job-id
                                       :states states
                                       :page-number page-number})]
    {:venia/queries (concat [(if (not-empty vars)
                               [:applications vars app-fields]
                               [:applications app-fields])]
                            (when (= 1 page-number)
                              [[:jobs {:filter_type "all"
                                       :manager     manager-email
                                       :page_size   sub-db/jobs-page-size
                                       :page_number 1}
                                [:id :slug :title :companyId
                                 [:company [:name]]]]
                               [:admin_companies [:id :name]]]))}))

(defn application-state-frequencies-query
  [job-id company-id]
  (let [vars (merge {}
                    (when job-id {:job_id job-id})
                    (when company-id {:company_id company-id}))]
    {:venia/queries [(if (not-empty vars)
                       [:application_state_frequencies vars [:state :count]]
                       [:application_state_frequencies [:state :count]])]}))

(defn fetch-applications-by-job-query
  [job-id company-id admin?]
  {:venia/queries [[:applications
                    (applications-query-args {:job-id      job-id
                                              :company-id  company-id
                                              :page-number 1
                                              :page-size   3
                                              :states      (if admin? [:pending] [:approved])})
                    base-application-fields]
                   (-> (application-state-frequencies-query job-id company-id)
                       :venia/queries
                       (first))]})

(reg-event-fx
  ::fetch-application-state-frequencies
  db/default-interceptors
  (fn [{db :db} [job-id-or-nil]]
    (let [company-id (sub-db/company-id db)]
      {:graphql {:query (application-state-frequencies-query job-id-or-nil company-id)
                 :on-success [::fetch-application-state-frequencies-success]
                 :on-failure [::fetch-application-state-frequencies-failure job-id-or-nil]}})))

(reg-event-db
  ::fetch-application-state-frequencies-success
  company-interceptors
  (fn [db [resp]]
    (let [asf (get-in resp [:data :application_state_frequencies])]
      (assoc db ::sub-db/frequencies (reduce (fn [a {state :state freq :count}]
                                               (assoc a (keyword state) freq)) {} asf)))))

(reg-event-fx
  ::fetch-application-state-frequencies-failure
  company-interceptors
  (fn [{db :db} [job-id-or-nil _resp]]
    {:dispatch [:error/set-global "Something went wrong while we tried to fetch your data 😢"
                [::fetch-application-state-frequencies job-id-or-nil]]}))

(reg-event-fx
  ::fetch-company
  db/default-interceptors
  (fn [{db :db} [company-id]]
    {:graphql {:query {:venia/queries [[:company {:id company-id} [:logo :name :permissions]]]}
               :on-success [::fetch-company-success]
               :on-failure [::fetch-company-failure]}}))

(reg-event-fx
  ::fetch-company-success
  db/default-interceptors
  (fn [{db :db} [resp]]
    (let [company         (get-in resp [:data :company])
          perms           (set (map keyword (:permissions company)))
          has-permission? (or (user-common/admin? db) (contains? perms :can_see_applications))]
      (merge {:db (update db ::sub-db/sub-db
                          #(assoc %
                                  ::sub-db/has-permission? has-permission?
                                  ::sub-db/logo (:logo company)
                                  ::sub-db/company-name (:name company)))}
             (when-not has-permission? ;; if no permission, send back to payment flow
               {:navigate [:payment-setup :params {:step :select-package} :query-params {:action "applications"}]})))))

(reg-event-fx
  ::fetch-company-failure
  db/default-interceptors
  (fn [_ _]))

(reg-event-fx
  ::fetch-company-jobs
  db/default-interceptors
  (fn [{db :db} [company-id]]
    {:graphql {:query {:venia/queries [(all-company-jobs-query-fragment company-id sub-db/jobs-page-size 1 [:id :slug :title])]}
               :on-success [::fetch-company-jobs-success]
               :on-failure [::fetch-company-jobs-failure company-id]}}))

(reg-event-db
  ::fetch-company-jobs-success
  company-interceptors
  (fn [db [resp]]
    (let [jobs (get-in resp [:data :jobs])]
      (assoc db ::sub-db/jobs jobs))))

(reg-event-fx
  ::fetch-company-jobs-failure
  db/default-interceptors
  (fn [_ [company-id]]
    {:dispatch [:error/set-global "Something went wrong while we tried to fetch your data 😢"
                [::fetch-company-jobs company-id]]}))

(defn parse-timestamp
  [x]
  (when-not (str/blank? x)
    (tf/parse (tf/formatters :date-time) x)))

(defn transform-application
  [{:keys [other-urls current-location preferred-locations] :as app}]
  (let [other-urls (url/detect-urls-type other-urls)]
    (-> app
        (assoc :other-urls other-urls
               :github (->> other-urls
                            (filter #(= (:type %) :github))
                            first
                            :display)
               :display-location
               (when (or current-location (seq preferred-locations))
                 (format-job-location (or current-location (first preferred-locations)) false)))
        (update :timestamp parse-timestamp))))

(defn transform-job
  [job]
  (-> job
      (cases/->kebab-case)
      (assoc :display-location (format-job-location (:location job) false))
      (update :first-published parse-timestamp)))

(reg-event-fx
  ::fetch-more-applications
  db/default-interceptors
  (fn [{db :db} _]
    {:db (update-in db [::sub-db/sub-db ::sub-db/current-page] inc)
     :dispatch-n [[::set-applications-loading true]
                  [::fetch-applications (get-in db [::db/query-params "job-id"])]]}))

(reg-event-fx
  ::fetch-more-latest-applied-jobs
  company-interceptors
  (fn [{db :db} _]
    {:db (update db ::sub-db/current-page inc)
     :dispatch-n [[::set-applications-loading true]
                  [::fetch-latest-applied-jobs]]}))

(reg-event-fx
  ::fetch-applications
  db/default-interceptors
  (fn [{db :db} [job-id-or-nil]]
    (let [company-id          (sub-db/company-id db)
          admin?              (user-common/admin? db)
          page-number         (get-in db [::sub-db/sub-db ::sub-db/current-page])
          states              (sub-db/tab->states (get-in db [::sub-db/sub-db ::sub-db/current-tab]) admin?)
          admin-applications? (and admin?  (not company-id))
          query               (if admin-applications?
                                (admin-applications-query job-id-or-nil (get-in db [::user/sub-db ::user/email]) states page-number)
                                (applications-query {:job-id job-id-or-nil :company-id company-id :states states :page-number page-number}))]
      {:graphql {:query      query
                 :on-success [::fetch-applications-success admin-applications? job-id-or-nil page-number]
                 :on-failure [::fetch-applications-failure]}})))

(reg-event-fx
  ::fetch-applications-success
  company-interceptors
  (fn [{db :db} [admin-applications? job-id page-number resp]]
    (let [fresh?        (= 1 page-number)
          jobs          (mapv transform-job (get-in resp [:data :jobs]))
          companies     (when admin-applications? (get-in resp [:data :admin_companies]))
          logo          (get-in resp [:data :company :logo])
          company-name  (get-in resp [:data :company :name])
          fetch-company (when (not logo) (some #(when (= job-id (:id %)) (:company-id %)) jobs))
          applications  (as-> resp x
                          (get-in x [:data :applications])
                          (map #(as-> % item
                                  (merge item (:user item))
                                  (dissoc item :user)
                                  (cases/->kebab-case item)
                                  (transform-application item))
                               x))]
      (merge {:db (assoc db
                         ::sub-db/applications (if fresh? applications (concat (::sub-db/applications db) applications))
                         ::sub-db/jobs (if fresh? jobs (concat (::sub-db/jobs db) jobs))
                         ::sub-db/logo (if fresh? logo (or (::sub-db/logo db) logo))
                         ::sub-db/company-name (if fresh? company-name (or (::sub-db/company-name db) company-name))
                         ::sub-db/companies (if fresh? companies (concat (::sub-db/companies db) companies)))}
             (if fetch-company
               {:dispatch-n [[::set-applications-loading false]
                             [::fetch-company fetch-company]]}
               {:dispatch [::set-applications-loading false]})))))

(reg-event-fx
  ::fetch-applications-failure
  company-interceptors
  (fn [{db :db} [job-id-or-nil _resp]]
    {:dispatch-n [[::set-applications-loading false]
                  [:error/set-global "Something went wrong while we tried to fetch your data 😢"
                   [::fetch-applications job-id-or-nil]]]}))

(reg-event-fx
  ::fetch-latest-applied-jobs
  db/default-interceptors
  (fn [{db :db} _]
    (let [company-id (sub-db/company-id db)
          page       (get-in db [::sub-db/sub-db ::sub-db/current-page])]
      {:graphql {:query      (latest-applied-company-jobs-query company-id page)
                 :on-success [::fetch-latest-applied-jobs-success page]
                 :on-failure [::fetch-latest-applied-jobs-failure]}})))

(reg-event-fx
  ::fetch-latest-applied-jobs-success
  company-interceptors
  (fn [{db :db} [page-number resp]]
    (let [fresh? (= 1 page-number)
          jobs   (mapv transform-job (get-in resp [:data :latest_applied_jobs :jobs]))
          total  (get-in resp [:data :latest_applied_jobs :pagination :total])]
      {:db         (assoc db
                          ::sub-db/latest-applied-jobs (if fresh? jobs (concat (::sub-db/latest-applied-jobs db) jobs))
                          ::sub-db/total-latest-applied-jobs total)
       :dispatch-n [[::set-applications-loading false]
                    [::fetch-batch-applications-by-job (map :id jobs)]]})))

(reg-event-fx
  ::fetch-latest-applied-jobs-failure
  company-interceptors
  (fn [{db :db} [resp]]
    {:dispatch-n [[::set-applications-loading false]
                  [:error/set-global "Something went wrong while we tried to fetch your data 😢"
                   [::fetch-latest-applied-jobs]]]}))

(reg-event-fx
  ::fetch-batch-applications-by-job
  company-interceptors
  (fn [{db :db} [job-ids]]
    {:db (update db ::sub-db/applications-by-job #(reduce (fn [a job-id] (assoc-in a [job-id :current-tab] :pending)) % job-ids))
     :dispatch-n (mapv (partial vector ::fetch-applications-by-job ) job-ids)}))

(reg-event-fx
  ::fetch-applications-by-job
  db/default-interceptors
  (fn [{db :db} [job-id]]
    {:db (update-in db [::sub-db/sub-db ::sub-db/applications-by-job job-id] merge {:loading? true})
     :graphql {:query      (fetch-applications-by-job-query job-id (sub-db/company-id db) (user-common/admin? db))
               :on-success [::fetch-applications-by-job-success job-id]
               :on-failure [::fetch-applications-by-job-failure job-id]}}))

(reg-event-fx
  ::fetch-applications-by-job-success
  company-interceptors
  (fn [{db :db} [job-id resp]]
    (let [applications (mapv transform-job (get-in resp [:data :applications]))
          asf (get-in resp [:data :application_state_frequencies])]
      {:db (update-in db [::sub-db/applications-by-job job-id] merge
                      {:loading? false
                       :applications (map #(as-> % item
                                             (merge item (:user item))
                                             (dissoc item :user)
                                             (cases/->kebab-case item)
                                             (transform-application item))
                                          applications)
                       :frequencies (reduce (fn [a {state :state freq :count}]
                                              (assoc a (keyword state) freq)) {} asf)})})))

(reg-event-fx
  ::fetch-applications-by-job-failure
  company-interceptors
  (fn [{db :db} [job-id resp]]
    {:dispatch [:error/set-global "Something went wrong while we tried to fetch your data 😢"
                [::fetch-applications-by-job job-id]]}))

(reg-event-fx
  ::select-job
  db/default-interceptors
  (fn [{db :db} [job-id]]
    (let [company-id (sub-db/company-id db)]
      {:navigate [(sub-db/get-current-page db)
                  :params (when company-id
                            {:id company-id})
                  :query-params (when job-id {:job-id job-id})]})))

(reg-event-db
  ::set-applications-loading
  company-interceptors
  (fn [db [loading?]]
    (assoc db ::sub-db/applications-loading? loading?)))

(reg-event-fx
  ::select-tab
  db/default-interceptors
  (fn [{db :db} [tab]]
    (let [job-id (get-in db [::db/query-params "job-id"])
          company-id (sub-db/company-id db)
          qps (merge {:tab (name tab)}
                     (when job-id {:job-id job-id}))
          page (sub-db/get-current-page db)]
      {:navigate (if (= page :admin-company-applications)
                   [page
                    :params {:id company-id}
                    :query-params qps]
                   [page
                    :query-params qps])
       :db (-> db
               (assoc-in [::sub-db/sub-db ::sub-db/current-tab] tab)
               ;; jettison applications; we'll be getting new ones
               (update ::sub-db/sub-db dissoc ::sub-db/applications))})))

(reg-event-fx
  ::select-company
  db/default-interceptors
  (fn [_ [company-id]]
    {:navigate [:admin-company-applications :params {:id company-id}]}))

(reg-event-fx
  ::set-application-state
  db/default-interceptors
  (fn [{db :db} [job-id-or-ids user-id state]]
    (merge {:graphql {:query queries/set-application-state-mutation
                      :variables {:input {:user_id user-id
                                          :job_ids (util/->vec job-id-or-ids)
                                          :action state}}
                      :on-success [::set-application-state-success job-id-or-ids user-id (action->state state)]
                      :on-failure [::set-application-state-failure job-id-or-ids user-id (action->state state)]}}
           (when (sub-db/company-view? db)
             {:db (assoc-in db [::sub-db/sub-db ::sub-db/applications-by-job job-id-or-ids :loading?] true)}))))

(defn get-user-and-job
  [db user-id job-id]
  (if-let [applications (not-empty (get-in db [::sub-db/sub-db ::sub-db/applications]))]
    {:user (some #(when (= user-id (:user-id %)) %) applications)
     :job  (some #(when (= job-id (:job-id %)) %) applications)}
    (let [job (get-in db [::sub-db/sub-db ::sub-db/applications-by-job job-id])]
      {:job job
       :user (some #(when (= user-id (:user-id %)) %) (:applications job))})))

(defn remove-application
  [db user-id job-id-or-ids]
  (update-in db
             [::sub-db/sub-db ::sub-db/applications]
             (partial remove #(and (or (nil? job-id-or-ids)
                                       (= job-id-or-ids (:job-id %))
                                       (contains? job-id-or-ids (:job-id %)))
                                   (= user-id (:user-id %))))))

(reg-event-fx
  ::set-application-state-success
  db/default-interceptors
  (fn [{db :db} [job-id-or-ids user-id state _resp]]
    (let [user-and-job (get-user-and-job db user-id (when-not (sequential? job-id-or-ids) job-id-or-ids))]
      ;; remove the application because it's no longer in this state
      ;; (only applicable for 'applications' whereas we just re-query 'applications-by-job' in the case of company view)
      (merge {:db (remove-application db user-id job-id-or-ids)
              :dispatch-n (concat (if (sub-db/company-view? db)
                                    [[::fetch-applications-by-job job-id-or-ids]]
                                    ;; TODO this is actually incorrect if a job-id is present;
                                    ;; we only want state frequencies for this job, but ::fetch-applications-by-job
                                    ;; is a special event, only to be used on the company view (epic fail?)
                                    [[::fetch-application-state-frequencies]])
                                  (when (and job-id-or-ids (= state "get_in_touch") (not (user-common/admin? db)))
                                    [[::show-get-in-touch-overlay user-and-job]]))}))))

(reg-event-fx
  ::set-application-state-failure
  db/default-interceptors
  (fn [{db :db} [job-id-or-ids user-id state _resp]]
    (merge {:dispatch-n [[:error/set-global "Something went wrong while we tried to change this application's state 😢"
                          [::set-application-state job-id-or-ids user-id state]]]}
           (when (sub-db/company-view? db)
             {:db (assoc-in db [::sub-db/sub-db ::sub-db/applications-by-job job-id-or-ids :loading?] false)}))))

(reg-event-db
  ::show-get-in-touch-overlay
  company-interceptors
  (fn [db [user-and-job]]
    (assoc db ::sub-db/get-in-touch-overlay user-and-job)))

(reg-event-db
  ::hide-get-in-touch-overlay
  company-interceptors
  (fn [db _]
    (dissoc db ::sub-db/get-in-touch-overlay)))

(reg-event-db
  ::show-job-selection-overlay
  company-interceptors
  (fn [db [user-id state]]
    (assoc db ::sub-db/job-selection-overlay-args [user-id state])))

(reg-event-fx
  ::hide-job-selection-overlay
  company-interceptors
  (fn [{db :db} [dismissed?]]
    (merge {:db (dissoc db
                        ::sub-db/job-selection-overlay-args
                        ::sub-db/job-selection-overlay-job-selections)}
           (when-not dismissed?
             (when-let [job-ids (not-empty (::sub-db/job-selection-overlay-job-selections db))]
               (let [[user-id state] (::sub-db/job-selection-overlay-args db)]
                 {:dispatch [::set-application-state
                             job-ids
                             user-id
                             state]}))))))

(reg-event-db
  ::toggle-job-for-application-state
  company-interceptors
  (fn [db [job-id]]
    (update db ::sub-db/job-selection-overlay-job-selections util/toggle job-id)))

(reg-event-db
  ::show-notes-overlay
  db/default-interceptors
  (fn [db [user-id job-id]]
    (assoc-in db [::sub-db/sub-db ::sub-db/notes-overlay-args]
              [user-id job-id (if (sub-db/company-view? db)
                                (some-> (sub-db/some-application-by-job (::sub-db/sub-db db) job-id #(= user-id (:user-id %))) :note)
                                (some #(when (and (= user-id (:user-id %))
                                                  (= job-id (:job-id %))) (:note %))
                                      (::sub-db/applications db)))])))

(reg-event-fx
  ::hide-notes-overlay
  db/default-interceptors
  (fn [{db :db} [revert?]]
    (let [[user-id job-id original-note] (get-in db [::sub-db/sub-db ::sub-db/notes-overlay-args])
          note             (when-not revert?
                             (if (sub-db/company-view? db)
                               (some-> (sub-db/some-application-by-job (::sub-db/sub-db db) job-id #(= user-id (:user-id %))) :note)
                               (some #(when (and (= user-id (:user-id %))
                                                 (= job-id (:job-id %))) (:note %))
                                     (get-in db [::sub-db/sub-db ::sub-db/applications]))))]
      {:db      (update db ::sub-db/sub-db dissoc ::sub-db/notes-overlay-args)
       :dispatch (if revert?
                   [::edit-note user-id job-id original-note]
                   [::set-application-note user-id job-id note])})))

(reg-event-fx
  ::set-application-note
  company-interceptors
  (fn [{db :db} [user-id job-id note]]
    {:graphql {:query      queries/set-application-note-mutation
               :variables  {:job_id  job-id
                            :user_id user-id
                            :note    note}
               :on-success [::set-application-note-success user-id job-id note]
               :on-failure [::set-application-note-failure user-id job-id note]}}))

(reg-event-fx
  ::set-application-note-failure
  company-interceptors
  (fn [{db :db} [user-id job-id note _resp]]
    {:dispatch-n [[:error/set-global "Something went wrong while we tried to change this application's note 😢"
                   [::set-application-note user-id job-id note]]]}))

(reg-event-fx
  ::set-application-note-success
  company-interceptors
  (fn [{db :db} _args]
    {:db db}))

(reg-event-db
  ::edit-note
  company-interceptors
  (fn [db [user-id job-id note]]
    ;; TODO remove parallel containers for applications
    (-> db
        (update ::sub-db/applications (fn [apps] (map #(if (and (= user-id (:user-id %))
                                                                (= job-id (:job-id %)))
                                                         (assoc % :note (when-not (str/blank? note) note))
                                                         %) apps)))
        (sub-db/update-applications-by-job job-id #(if (= user-id (:user-id %))
                                                     (assoc % :note (when-not (str/blank? note) note))
                                                     %)))))

(reg-event-db
  ::initialize-db
  db/default-interceptors
  (fn [db _]
    (assoc db ::sub-db/sub-db (sub-db/default-db db))))

(defn on-applications-page-load
  [db job-id]
  (concat [[::initialize-db]]
          (cond
            (sub-db/company-view? db)
            (let [company-id (sub-db/company-id db)]
              [[::fetch-latest-applied-jobs]
               [::fetch-company company-id]
               [::fetch-company-jobs company-id]])
            job-id
            [[::fetch-application-state-frequencies job-id]
             [::fetch-applications job-id]]
            :else ;; admin view
            [[::fetch-application-state-frequencies]
             [::fetch-applications]])))

(defmethod on-page-load :company-applications [db]
  (on-applications-page-load db (get-in db [::db/query-params "job-id"])))

(defmethod on-page-load :admin-company-applications [db]
  (on-applications-page-load db (get-in db [::db/query-params "job-id"])))

(defmethod on-page-load :admin-applications [db]
  (on-applications-page-load db nil))
