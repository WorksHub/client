(ns wh.logged-in.apply.events
  (:require
    [re-frame.core :refer [reg-event-db reg-event-fx dispatch path]]
    [wh.common.graphql-queries :as graphql]
    [wh.logged-in.profile.location-events :as location-events]
    [wh.db :as db]
    [wh.logged-in.apply.db :as apply]
    [wh.user.db :as user]
    [wh.util :as util])
  (:require-macros [wh.graphql-macros :refer [deffragment defquery def-query-template def-query-from-template]]))

(def apply-interceptors (into db/default-interceptors
                              [(path ::apply/sub-db)]))

(reg-event-db
  ::initialize-db
  apply-interceptors
  (fn [db _]
    (merge db apply/default-db)))

(reg-event-fx
  ::check-cv
  db/default-interceptors
  (fn [{db :db} [_]]
    (if (user/has-cv? db)
      {:dispatch [::apply]}
      {:db (-> db
               (assoc-in [::apply/sub-db ::apply/current-step] :cv-upload)
               (update-in [::apply/sub-db ::apply/steps-taken] (fnil conj #{}) :cv-upload))})))

(reg-event-fx
  ::check-name
  db/default-interceptors
  (fn [{db :db} [_]]
    (if (user/has-full-name? db)
      {:dispatch [::check-current-location]}
      {:db (-> db
               (assoc-in [::apply/sub-db ::apply/current-step] :name)
               (update-in [::apply/sub-db ::apply/steps-taken] (fnil conj #{}) :name))})))


(reg-event-fx
  ::check-current-location
  db/default-interceptors
  (fn [{db :db} [_]]
    (if (user/has-current-location? db)
      {:dispatch [::check-cv]}
      {:db (-> db
               (assoc-in [::apply/sub-db ::apply/current-step] :current-location)
               (update-in [::apply/sub-db ::apply/steps-taken] (fnil conj #{}) :current-location))})))


(reg-event-fx
  :apply/start-apply-for-job
  db/default-interceptors
  (fn [{db :db} [job]]
    {:db              (assoc-in db [::apply/sub-db ::apply/job] job)
     :dispatch        [::check-name]
     :analytics/track ["Job Application Started" job]}))

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

(reg-event-db
  ::cv-upload-start
  apply-interceptors
  (fn [db _]
    (assoc db ::apply/updating? true)))

(reg-event-fx
  ::cv-upload-success
  db/default-interceptors
  (fn [{db :db} [filename {:keys [url]}]]
    {:graphql {:query      graphql/update-user-mutation--cv
               :variables  {:update_user {:id (get-in db [::user/sub-db ::user/id])
                                          :cv {:file {:name filename, :url url}}}}
               :on-success [::cv-update-url-success]
               :on-failure [::cv-upload-failure]}}))


(reg-event-fx
  ::cv-update-url-success
  db/default-interceptors
  (fn [{db :db} [resp]]
    {:db       (-> db
                   (assoc-in [::user/sub-db ::user/cv :file] (get-in resp [:data :update_user :cv :file]))
                   (assoc-in [::apply/sub-db ::apply/updating?] false)
                   (assoc-in [::apply/sub-db ::apply/cv-upload-failed?] false))
     :dispatch [::check-cv]}))


(reg-event-fx
  ::cv-upload-failure
  apply-interceptors
  (fn [{db :db} _]
    {:db (assoc db
           ::apply/updating? false
           ::apply/cv-upload-failed? true)}))

(reg-event-fx
  ::update-name-success
  db/default-interceptors
  (fn [{db :db} [resp]]
    {:db       (-> db
                   (assoc-in [::user/sub-db ::user/name] (get-in resp [:data :update_user :name]))
                   (assoc-in [::apply/sub-db ::apply/updating?] false))
     :dispatch [::check-name]}))

(reg-event-db
  ::update-name-failure
  apply-interceptors
  (fn [db _]
    (assoc db
      ::apply/updating? false
      ::apply/name-update-failed? true)))

(reg-event-fx
  ::update-name
  db/default-interceptors
  (fn [{db :db} [name]]
    {:db      (assoc-in db [::apply/sub-db ::apply/updating?] true)
     :graphql {:query      graphql/update-user-mutation--name
               :variables  {:update_user {:id   (get-in db [::user/sub-db ::user/id])
                                          :name name}}
               :on-success [::update-name-success]
               :on-failure [::update-name-failure]}}))

(reg-event-fx
  ::update-visa-success
  db/default-interceptors
  (fn [{db :db} [resp]]
    {:db       (-> db
                   (assoc-in [::user/sub-db ::user/visa-status] (get-in resp [:data :update_user :visaStatus]))
                   (assoc-in [::user/sub-db ::user/visa-status-other] (get-in resp [:data :update_user :visaStatusOther]))
                   (assoc-in [::apply/sub-db ::apply/updating?] false))
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
               :variables  {:update_user (cond-> {:id         (get-in db [::user/sub-db ::user/id])
                                                  :visaStatus visa-status}
                                                 (contains? visa-status "Other") (merge {:visaStatusOther visa-other}))}
               :on-success [::update-visa-success]
               :on-failure [::update-visa-failure]}}))

(reg-event-fx
  ::check-visa
  db/default-interceptors
  (fn [{db :db} [_]]
    {:db (assoc-in db [::apply/sub-db ::apply/current-step] (if (user/has-visa? db) :thanks :visa))}))

(reg-event-fx
  ::update-current-location-success
  db/default-interceptors
  (fn [{db :db} [resp]]
    {:db       (-> db
                   (assoc-in [::user/sub-db ::user/current-location] (get-in resp [:data :update_user :currentLocation]))
                   (assoc-in [::apply/sub-db ::apply/updating?] false))
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
      {:db      (assoc-in db [::apply/sub-db ::apply/updating?] true)
       :graphql {:query      graphql/update-user-mutation--current-location
                 :variables  {:update_user (util/transform-keys {:id               (get-in db [::user/sub-db ::user/id])
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


(reg-event-fx
  ::handle-apply
  apply-interceptors
  (fn [{db :db} [success? resp]]
    (cond-> {:db (assoc db ::apply/submit-success? success?
                           ::apply/updating? false
                           ::apply/error (when-not success?
                                           (util/gql-errors->error-key resp)))}
            success? (assoc :dispatch-n [[::check-visa]
                                         [:wh.jobs.job.events/set-applied]]
                            :analytics/track ["Job Applied" (::apply/job db)]))))

(defquery apply-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "Apply"}
   :venia/variables [{:variable/name "id"
                      :variable/type :String!}]
   :venia/queries   [[:apply {:id :$id}]]})

(reg-event-fx
  ::apply
  apply-interceptors
  (fn [{db :db} _]
    {:graphql {:query      apply-mutation
               :variables  {:id (get-in db [::apply/job :id])}
               :on-success [::handle-apply true]
               :on-failure [::handle-apply false]}
     :db      (assoc db ::apply/updating? true)}))
