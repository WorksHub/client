(ns wh.logged-in.apply.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx dispatch path]]
            [wh.common.cases :as cases]
            [wh.common.graphql-queries :as graphql]
            [wh.common.keywords :as keywords]
            [wh.db :as db]
            [wh.events :as events]
            [wh.graphql.jobs :as graphql-jobs]
            [wh.logged-in.apply.db :as apply]
            [wh.logged-in.profile.location-events :as location-events]
            [wh.user.db :as user]
            [wh.util :as util])
  (:require-macros [wh.graphql-macros :refer [deffragment defquery def-query-template def-query-from-template]]))

(def apply-interceptors (into db/default-interceptors
                              [(path ::apply/sub-db)]))

(def chat-elm-id "chatbot")
(def scroll-to-bottom [::events/scroll-to-bottom chat-elm-id])

;; MISC ─────────────────────────────────────────────────────────────────────────────

(reg-event-db
  ::initialize-db
  apply-interceptors
  (fn [db _]
    (merge db apply/default-db)))


(reg-event-fx
  ::track-recommendations-redirect
  apply-interceptors
  (fn [_ _]
    {:analytics/track ["Recommendations page opened"]}))

(reg-event-db
  ::close-chatbot
  apply-interceptors
  (fn [_ _]
    apply/default-db))

;; START JOB APPLICATION ─────────────────────────────────────────────────────────────────────────────

(reg-event-fx
  :apply/start-apply-for-job
  db/default-interceptors
  (fn [{db :db} [job apply-source]]
    (let [job (assoc job :apply-source apply-source)]
      {:db              (assoc-in db [::apply/sub-db ::apply/job] job)
       :dispatch-n      [[::check-name]
                         [::fetch-job-company-details job]]
       :analytics/track ["Job Application Started" job]})))

(reg-event-fx
  ::fetch-job-company-details
  db/default-interceptors
  (fn [{db :db} [job]]
    {:graphql {:query      graphql-jobs/job-query--company-managed-details
               :variables  job
               :on-success [::fetch-job-company-details-success]}}))

(reg-event-db
  ::fetch-job-company-details-success
  apply-interceptors
  (fn [db [{{{company :company} :job} :data}]]
    (assoc db
           ::apply/company-managed? (:managed company)
           ::apply/company-name     (:name company))))

;; CV ─────────────────────────────────────────────────────────────────────────────

(reg-event-fx
  ::check-cv
  db/default-interceptors
  (fn [{db :db} [_]]
    (if (user/has-cv? db)
      {:dispatch [::check-skills]}
      {:db (-> db
               (apply/update-current-step :step/cv-upload)
               (apply/update-taken-steps :step/cv-upload))
       :dispatch scroll-to-bottom})))

(reg-event-db
  ::cv-upload-start
  db/default-interceptors
  (fn [db _]
    (apply/set-loading db)))

(reg-event-fx
  ::cv-upload-success
  db/default-interceptors
  (fn [{db :db} [filename {:keys [url hash]}]]
    {:graphql {:query      graphql/update-user-mutation--cv
               :variables  {:update_user {:id (user/id db)
                                          :cv {:file {:name filename :url url :hash hash}}}}
               :on-success [::cv-update-url-success]
               :on-failure [::cv-upload-failure]}}))

(reg-event-fx
  ::cv-update-url-success
  db/default-interceptors
  (fn [{db :db} [resp]]
    {:db       (-> db
                   (user/update-cv (get-in resp [:data :update_user :cv :file]))
                   (apply/unset-loading)
                   (assoc-in [::apply/sub-db ::apply/cv-upload-failed?] false))
     :dispatch [::check-cv]}))

(reg-event-fx
  ::cv-upload-failure
  apply-interceptors
  (fn [{db :db} _]
    {:db (assoc db
           ::apply/updating? false
           ::apply/cv-upload-failed? true)}))

;; NAME ─────────────────────────────────────────────────────────────────────────────

(reg-event-fx
  ::check-name
  db/default-interceptors
  (fn [{db :db} [_]]
    (if (user/has-full-name? db)
      {:dispatch [::check-current-location]}
      {:db (-> db
               (apply/update-current-step :step/name)
               (apply/update-taken-steps :step/name))
       :dispatch scroll-to-bottom})))

(reg-event-fx
  ::update-name
  db/default-interceptors
  (fn [{db :db} [name]]
    {:db      (apply/set-loading db)
     :graphql {:query      graphql/update-user-mutation--name
               :variables  {:update_user {:id   (user/id db)
                                          :name name}}
               :on-success [::update-name-success]
               :on-failure [::update-name-failure]}}))

(reg-event-fx
  ::update-name-success
  db/default-interceptors
  (fn [{db :db} [resp]]
    {:db       (-> db
                   (user/update-name (get-in resp [:data :update_user :name]))
                   (apply/unset-loading))
     :dispatch [::check-name]}))

(reg-event-db
  ::update-name-failure
  apply-interceptors
  (fn [db _]
    (assoc db
      ::apply/updating? false
      ::apply/name-update-failed? true)))

;; VISA ─────────────────────────────────────────────────────────────────────────────

(reg-event-fx
  ::check-visa
  db/default-interceptors
  (fn [{db :db} [_]]
    (if (user/has-visa? db)
      {:dispatch [::check-application]}
      {:db (apply/update-current-step db :step/visa)})))

(reg-event-fx
  ::update-visa-success
  db/default-interceptors
  (fn [{db :db} [resp]]
    {:db       (-> db
                   (user/update-visa-status (get-in resp [:data :update_user :visaStatus]))
                   (user/update-visa-status-other (get-in resp [:data :update_user :visaStatusOther]))
                   (apply/unset-loading))
     :dispatch [::check-visa]}))

(reg-event-db
  ::update-visa-failure
  apply-interceptors
  (fn [db _]
    (assoc db
      ::apply/updating? false
      ::apply/visa-update-failed? true)))


(reg-event-fx
  ::update-visa
  db/default-interceptors
  (fn [{db :db} [visa-status visa-other]]
    {:db      (assoc-in db [::apply/sub-db ::apply/updating?] true)
     :graphql {:query      graphql/update-user-mutation--visa-status
               :variables  {:update_user (cond-> {:id         (user/id db)
                                                  :visaStatus visa-status}
                                                 (contains? visa-status "Other") (merge {:visaStatusOther visa-other}))}
               :on-success [::update-visa-success]
               :on-failure [::update-visa-failure]}}))

;; COVER LETTER ─────────────────────────────────────────────────────────────────────────────

(def set-application-cover-letter-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "set_application_cover_letter"}
   :venia/variables [{:variable/name "job_id"       :variable/type :String!}
                     {:variable/name "user_id"      :variable/type :ID!}
                     {:variable/name "cover_letter" :variable/type :cover_letter_input!}]
   :venia/queries   [[:set_application_cover_letter {:job_id       :$job_id
                                                     :user_id      :$user_id
                                                     :cover_letter :$cover_letter}
                      [:status]]]})

(reg-event-db
 ::add-cover-letter
 db/default-interceptors
 (fn [db _]
   (apply/update-current-step db :step/cover-letter)))

(reg-event-db
  ::discard-cover-letter
  db/default-interceptors
  (fn [db _]
    (apply/update-current-step db :step/thanks)))

(reg-event-db
  ::cover-letter-upload-start
  apply-interceptors
  (fn [db _]
    (assoc db ::apply/updating? true)))

(reg-event-fx
  ::cover-letter-upload-success
  db/default-interceptors
  (fn [{db :db} [name {url :url hash :hash}]]
    {:dispatch [::set-cover-letter {:file {:name name :url url :hash hash}}]
     :db (assoc-in db [::apply/sub-db ::apply/cover-letter-upload-failed?] false)}))

(reg-event-db
  ::cover-letter-upload-failure
  db/default-interceptors
  (fn [db]
    (-> db
        (apply/unset-loading)
        (assoc-in [::apply/sub-db ::apply/cover-letter-upload-failed?] true))))

(reg-event-fx
  ::set-cover-letter
  db/default-interceptors
  (fn [{db :db} [cover-letter]]
    (let [job-id  (get-in db [:wh.job.db/sub-db :wh.job.db/id])
          user-id (get-in db [:wh.user.db/sub-db :wh.user.db/id])]
      {:graphql {:query      set-application-cover-letter-mutation
                 :variables  {:job_id       job-id
                              :user_id      user-id
                              :cover_letter cover-letter}
                 :on-success [::set-cover-letter-success]
                 :on-failure [::set-cover-letter-failure]}
       :db      (apply/set-loading db)})))

(reg-event-db
  ::set-cover-letter-success
  db/default-interceptors
  (fn [db _]
    (-> db
        (assoc-in [::apply/sub-db ::apply/cover-letter-upload-failed?] false)
        (apply/update-current-step :step/thanks))))

(reg-event-db
  ::set-cover-letter-failure
  db/default-interceptors
  (fn [db _]
    (assoc-in db [::apply/sub-db ::apply/cover-letter-upload-failed?] true)))

;; LOCATION ─────────────────────────────────────────────────────────────────────────────

(reg-event-fx
  ::check-current-location
  db/default-interceptors
  (fn [{db :db} [_]]
    (if (user/has-current-location? db)
      {:dispatch [::check-cv]}
      {:db (-> db
               (apply/update-current-step :step/current-location)
               (apply/update-taken-steps :step/current-location))
       :dispatch scroll-to-bottom})))

(reg-event-fx
  ::update-current-location-success
  db/default-interceptors
  (fn [{db :db} [resp]]
    {:db       (-> db
                   (assoc-in [::user/sub-db ::user/current-location] (get-in resp [:data :update_user :currentLocation]))
                   (apply/unset-loading))
     :dispatch [::check-current-location]}))

(reg-event-db
  ::update-current-location-failure
  apply-interceptors
  (fn [db _]
    (assoc db
      ::apply/updating? false
      ::apply/current-location-update-failed? true)))

(reg-event-fx
  ::update-current-location
  db/default-interceptors
  (fn [{db :db} _]
    (let [current-location (get-in db [::apply/sub-db ::apply/current-location])]
      {:db      (apply/set-loading db)
       :graphql {:query      graphql/update-user-mutation--current-location
                 :variables  {:update_user (keywords/transform-keys {:id               (user/id db)
                                                                 :current-location current-location})}
                 :on-success [::update-current-location-success]
                 :on-failure [::update-current-location-failure]}})))

(reg-event-fx
  ::current-location-search-success
  apply-interceptors
  (fn [{db :db} [result]]
    {:db (assoc db ::apply/current-location-suggestions result)}))

(reg-event-fx
  ::edit-current-location
  apply-interceptors
  (fn [{:keys [db]} [loc]]
    {:db       (assoc db ::apply/current-location-text loc)
     :dispatch [::location-events/search {:query      loc
                                          :on-success [::current-location-search-success]
                                          :on-failure []}]}))

(reg-event-db
  ::select-current-location-suggestion
  apply-interceptors
  (fn [db [item]]
    (assoc db
      ::apply/current-location item
      ::apply/current-location-text nil
      ::apply/current-location-suggestions nil)))

;; APPLICATION ─────────────────────────────────────────────────────────────────────────────

(defquery check-application-query
  {:venia/operation {:operation/type :query
                     :operation/name "check_application"}
   :venia/variables [{:variable/name "id"
                      :variable/type :String}
                     {:variable/name "slug"
                      :variable/type :String}]
   :venia/queries   [[:check_application {:id :$id
                                          :slug :$slug}
                      [:check_status :reason]]]})

(reg-event-fx
  ::check-application
  apply-interceptors
  (fn [{db :db} _]
    {:graphql {:query      check-application-query
               :variables  (or (some->> (get-in db [::apply/job :id]) (hash-map :id))
                               (some->> (get-in db [::apply/job :slug]) (hash-map :slug)))
               :on-success [::check-application-success]
               :on-failure []}
     :db      (assoc db ::apply/updating? true)}))

(defn gql-check-application->check-application [data]
  (-> data
      cases/->kebab-case
      (update :check-status keyword)
      (update :reason keyword)))

(reg-event-db
  ::check-application-success
  apply-interceptors
  (fn [db [{:keys [data]}]]
    (let [result (gql-check-application->check-application (:check_application data))]
      (if (= :rejected (:check-status result))
        (cond-> (assoc db ::apply/current-step :step/rejection
                          ::apply/updating? false)
                (:reason result) (assoc-in [::apply/rejection :reason] (:reason result)))
        (-> db
            (assoc ::apply/updating? false)
            (assoc ::apply/current-step :step/cover-letter))))))

;; SKILLS ─────────────────────────────────────────────────────────────────────────────

(reg-event-fx
  ::check-skills
  db/default-interceptors
  (fn [{db :db} [_]]
    (if (user/has-skills? db)
      {:dispatch [::apply]}
      {:db (-> db
               (apply/update-current-step :step/skills)
               (apply/update-taken-steps :step/skills))
       :dispatch scroll-to-bottom})))

(reg-event-fx
  ::update-skills
  db/default-interceptors
  (fn [{db :db} _]
    {:graphql {:query      graphql/update-user-mutation--skills
               :variables  {:update_user
                            (keywords/transform-keys {:id     (user/id db)
                                                  :skills (->> db
                                                               apply/sub-db
                                                               apply/selected-skills
                                                               (map #(hash-map :name %)))})}
               :on-success [::update-skills-success]
               :on-failure [::update-skills-failure]}
     :db      (-> db
                  apply/unset-skills-update-failed
                  apply/set-loading)}))

(reg-event-fx
  ::update-skills-success
  db/default-interceptors
  (fn [{db :db} [resp]]
    {:db (-> db
             (user/update-skills (get-in resp [:data :update_user :skills]))
             apply/unset-loading)
     :dispatch [::check-skills]}))

(reg-event-db
  ::update-skills-failure
  db/default-interceptors
  (fn [db _]
    (-> db
        apply/set-skills-update-failed
        apply/unset-loading)))

(reg-event-db
  ::toggle-skill
  apply-interceptors
  (fn [db [skill]]
    (apply/toggle-skill db skill)))

;; APPLY ─────────────────────────────────────────────────────────────────────────────

(defquery apply-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "Apply"}
   :venia/variables [{:variable/name "id"
                      :variable/type :String}
                     {:variable/name "slug"
                      :variable/type :String}
                     {:variable/name "applySource"
                      :variable/type :String}]
   :venia/queries   [[:apply {:id          :$id
                              :slug        :$slug
                              :applySource :$applySource}]]})

(reg-event-fx
  ::apply
  apply-interceptors
  (fn [{db :db} _]
    {:graphql {:query      apply-mutation
               :variables  (-> (or (some->> (get-in db [::apply/job :id])   (hash-map :id))
                                   (some->> (get-in db [::apply/job :slug]) (hash-map :slug)))
                               (assoc :applySource (:apply-source (::apply/job db))))
               :on-success [::handle-apply true]
               :on-failure [::handle-apply false]}
     :db      (assoc db ::apply/updating? true)}))

(reg-event-fx
  ::handle-apply
  apply-interceptors
  (fn [{db :db} [success? resp]]
    (cond-> {:db (assoc db
                   ::apply/submit-success? success?
                   ::apply/updating? false
                   ::apply/error (when-not success?
                                   (util/gql-errors->error-key resp)))}
            success? (assoc :dispatch-n [[::check-visa]
                                         [:wh.job.events/set-applied]]
                            :analytics/track ["Job Applied" (::apply/job db)]))))
