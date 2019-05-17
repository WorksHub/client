(ns wh.logged-in.apply.events
  (:require
    [re-frame.core :refer [reg-event-db reg-event-fx dispatch path]]
    [wh.common.graphql-queries :as graphql]
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
    (cond
      (user/has-cv? db) {:dispatch [::apply]}
      :else {:db (-> db
                     (assoc-in [::apply/sub-db ::apply/current-step] :cv-upload)
                     (update-in [::apply/sub-db ::apply/steps-taken] (fnil conj #{}) :cv-upload))})))

(reg-event-fx
  ::check-name
  db/default-interceptors
  (fn [{db :db} [_]]
    (cond
      (user/has-full-name? db) {:dispatch [::check-cv]}
      :else {:db (-> db
                     (assoc-in [::apply/sub-db ::apply/current-step] :name)
                     (update-in [::apply/sub-db ::apply/steps-taken] (fnil conj #{}) :name))})))

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
  ::handle-apply
  apply-interceptors
  (fn [{db :db} [success? resp]]
    (cond-> {:db (assoc db ::apply/submit-success? success?
                           ::apply/updating? false
                           ::apply/current-step :thanks
                           ::apply/error (when-not success?
                                           (util/gql-errors->error-key resp)))}
            success? (assoc :dispatch [:wh.jobs.job.events/set-applied]
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
