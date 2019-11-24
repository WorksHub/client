(ns wh.logged-in.apply.cancel-apply.events
  (:require [re-frame.core :refer [reg-event-fx reg-event-db path]]
            [wh.logged-in.apply.events :as apply-events]
            [wh.logged-in.apply.cancel-apply.db :as cancel]
            [wh.db :as db])
  (:require-macros [wh.graphql-macros :refer [deffragment defquery def-query-template def-query-from-template]]))

;; this is to handle the process of removing an application, this section has it's own folder
;; as it has it's own views and subs etc.

;; first the user clicks the "x" on the page , which will start the process and ::start-cancellation
;; will assoc ::apply/cancel-application *id*, which will trigger the codi message to pop up and ask
;; why they would want to quit , with reasons being "I've found work", "I changed my mind" etc.

;; after this they click submit, and that will fire the try-cancel-application which do the graphql thing
;; and on-success , dispatch the ::cancel-application-success event, which will dissoc the applied job out of
;; personalised, and take the id out of the :user/applied-jobs set.

;; job should be in same format used in application -> {:id id :company-name company-name :location location}
(def cancel-interceptors (into db/default-interceptors
                               [(path ::cancel/sub-db)]))

(reg-event-db
  ::initialize-db
  cancel-interceptors
  (fn [db _]
    (merge db cancel/default-db)))

(defn acceptable-reason?
  [reason]
  (and (string? reason)))

(reg-event-db
  ::set-reason
  cancel-interceptors
  (fn [db [reason]]
    (case reason
      "changed mind" (assoc db ::cancel/reason "changed mind")
      "found work" (assoc db ::cancel/reason "found work")
      (when (acceptable-reason? reason)
         (assoc db ::cancel/reason reason)))))

(reg-event-db
  ::start-cancellation
  cancel-interceptors
  (fn [db [job]]
    (let [id (:id job)
          company-name (:name (:company job))
          location (:location job)]
      (-> db
          (assoc ::cancel/job {:id id :company-name company-name :location location})
          (assoc ::cancel/current-step :reason)))))

(defquery cancel-application-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "Cancel"}
   :venia/variables [{:variable/name "id"
                      :variable/type :String}
                     {:variable/name "slug"
                      :variable/type :String}]

   ;; We could query apply and see if it is nil at this id,
   ;; but maybe nil isn't acceptable for every case? 
   :venia/queries   [[:apply {:id :$id
                              :slug :$slug}]]})

;; this is triggered by the conversation button
(reg-event-fx
 ::cancel-application
 cancel-interceptors
 (fn [{db :db} _]
   (let [job (get db ::cancel/job)
         id (some ->> (get job :id) (hash-map :id))
         slug (some->> (get job :slug) (hash-map :slug))]
     {:graphql {:query :cancel-application-mutation
                :variables (or id slug)
                :on-success [::cancel-application-success id slug]}})))

;; if failed, assoc :cancellation-failure, which should prompt codi-message of trying again.
(reg-event-fx
  ::cancel-application-success
  db/default-interceptors
  (fn [{db :db} [{:keys [data]} id slug]]
    (let [result (apply-events/gql-check-application->check-application data)]
      (if (= :failed (:check-status result))
        (cond-> (assoc db ::cancel/reason-failed? :failed)
          (:reason result) (assoc-in [::cancel/reason] (:reason result)))
        {:db (-> db
                 (assoc ::cancel/updating? true)
                 (assoc ::cancel/current-step :thanks)
                 (dissoc ::cancel/job))
         :dispatch [::remove-application id slug]}))))

(defn dissoc-applied-jobs
  [db id slug]
  (let [applied-jobs (get-in db [:wh.user.db/sub-db :wh.user.db/applied-jobs])]
    (or
     (disj applied-jobs id)
     (disj applied-jobs slug))))

(reg-event-fx
  ::remove-application
  db/default-interceptors
  (fn [{db :db} [id slug]]
    {:db (update-in db [:wh.user.db/sub-db :wh.user.db/applied-jobs] identity (dissoc-applied-jobs db id slug)
     :dispatch [:personalised-jobs/fetch-jobs-by-type :applied 1]}))


(reg-event-db
  ::close-chatbot
  cancel-interceptors
  (fn [_ _]
    cancel/default-db))
