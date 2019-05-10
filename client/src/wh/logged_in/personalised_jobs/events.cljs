(ns wh.logged-in.personalised-jobs.events
  (:require
    [re-frame.core :refer [dispatch path reg-event-db reg-event-fx]]
    [wh.common.job :as job]
    [wh.db :as db]
    [wh.graphql.jobs :as jobs]
    [wh.logged-in.personalised-jobs.db :as personalised-jobs]
    [wh.pages.core :refer [on-page-load] :as pages]
    ))

(def personalised-jobs-interceptors (into db/default-interceptors
                                          [(path ::personalised-jobs/sub-db)]))

(defn personalised-jobs-query [type-of-jobs page-number]
  {:venia/queries [[:jobs (merge
                            (when (= :recommended type-of-jobs)
                              {:entity_type "user"})
                            {:filter_type (name type-of-jobs)
                             :page_size   pages/default-page-size
                             :page_number page-number})
                    (conj jobs/job-card-fields :score)]]})

(reg-event-fx
  ::load-more
  personalised-jobs-interceptors
  (fn [{db :db} [type-of-jobs]]
    {:dispatch [:personalised-jobs/fetch-jobs-by-type type-of-jobs (inc (::personalised-jobs/current-page-number db))]}))

(reg-event-fx
  :personalised-jobs/fetch-jobs-by-type
  personalised-jobs-interceptors
  (fn [{db :db} [type-of-jobs page-number]]
    {:db (cond-> (assoc db ::personalised-jobs/current-page-number page-number)
                 (= 1 page-number) (assoc ::personalised-jobs/jobs []))
     :graphql {:query      (personalised-jobs-query type-of-jobs page-number)
               :on-success [::fetch-personalised-jobs-success]}
     :dispatch [::pages/set-loader]}))

(reg-event-fx
  ::fetch-personalised-jobs-success
  personalised-jobs-interceptors
  (fn [{{:keys [::personalised-jobs/jobs] :as db} :db} [{{new-jobs :jobs} :data}]]
    {:db       (assoc db ::personalised-jobs/jobs (concat jobs (mapv job/translate-job new-jobs))
                         ::personalised-jobs/show-load-more? (>= (count new-jobs) pages/default-page-size))
     :dispatch [::pages/unset-loader]}))

(defmethod on-page-load :recommended [db]
  [[::pages/set-loader]
   [:personalised-jobs/fetch-jobs-by-type :recommended 1]])

(defmethod on-page-load :liked [db]
  [[::pages/set-loader]
   [:personalised-jobs/fetch-jobs-by-type :liked 1]])
