(ns wh.logged-in.personalised-jobs.events
  (:require
    [re-frame.core :refer [dispatch path reg-event-db reg-event-fx]]
    [wh.common.cases :as cases]
    [wh.common.job :as job]
    [wh.db :as db]
    [wh.graphql.jobs :as jobs]
    [wh.logged-in.personalised-jobs.db :as personalised-jobs]
    [wh.pages.core :refer [on-page-load] :as pages]))


(def personalised-jobs-interceptors (into db/default-interceptors
                                          [(path ::personalised-jobs/sub-db)]))

(defn personalised-jobs-query [type-of-jobs page-number]
  (let [params (merge
                 (when (= :recommended type-of-jobs)
                   {:entity_type "user"})
                 {:filter_type (name type-of-jobs)
                  :page_size pages/default-page-size
                  :page_number page-number})
        fields (case type-of-jobs
                 :applied (remove #(= % :userScore) jobs/job-card-fields)
                 (conj jobs/job-card-fields :score))
        jobs-query [:jobs params fields]
        applications-query [:candidate_applications [:jobId :state]]]
    {:venia/queries (case type-of-jobs
                      :applied [jobs-query applications-query]
                      [jobs-query])}))

(reg-event-fx
  ::load-more
  personalised-jobs-interceptors
  (fn [{db :db} [type-of-jobs]]
    {:dispatch [:personalised-jobs/fetch-jobs-by-type type-of-jobs (inc (::personalised-jobs/current-page-number db))]}))

(reg-event-fx
  :personalised-jobs/fetch-jobs-by-type
  personalised-jobs-interceptors
  (fn [{db :db} [type-of-jobs page-number]]
    (merge
      {:db (cond-> (assoc db ::personalised-jobs/current-page-number page-number)
                   (= 1 page-number) (assoc ::personalised-jobs/jobs []))
       :graphql {:query      (personalised-jobs-query type-of-jobs page-number)
                 :on-success [::fetch-personalised-jobs-success]}}
      (when (= page-number 1)
       {:dispatch [::pages/set-loader]}))))

(defn add-application-state
  [jobs applications]
  (mapv (fn [{:keys [id] :as job}]
          (if-let [application (some #(when (= id (:job-id %)) %) applications)]
            (assoc job :state (keyword (:state application)))
            job)) jobs))

(reg-event-fx
  ::fetch-personalised-jobs-success
  personalised-jobs-interceptors
  (fn [{db :db} [resp]]
    (let [new-jobs (mapv job/translate-job (get-in resp [:data :jobs]))
          candidate-applications (mapv cases/->kebab-case (get-in resp [:data :candidate_applications]))
          jobs (::personalised-jobs/jobs db)
          updated-jobs (distinct (concat (map #(dissoc % :state) jobs) new-jobs))]
      {:db (assoc db ::personalised-jobs/jobs (if candidate-applications
                                                (add-application-state updated-jobs candidate-applications)
                                                updated-jobs)
                     ::personalised-jobs/show-load-more? (>= (count new-jobs) pages/default-page-size))
       :dispatch [::pages/unset-loader]})))

(defmethod on-page-load :recommended [db]
  [[::pages/set-loader]
   [:personalised-jobs/fetch-jobs-by-type :recommended 1]])

(defmethod on-page-load :liked [db]
  [[::pages/set-loader]
   [:personalised-jobs/fetch-jobs-by-type :liked 1]])

(defmethod on-page-load :applied [db]
  [[::pages/set-loader]
   [:personalised-jobs/fetch-jobs-by-type :applied 1]])
