(ns wh.company.events
  (:require
    [cljs.spec.alpha :as s]
    [wh.common.cases :as cases]
    [wh.common.specs.company]
    [wh.graphql.company :as gql]
    [wh.re-frame.events :refer [reg-event-fx reg-event-db]]))

(defn ->onboarding-tasks
  [db tasks]
  (let [tasks' (reduce (fn [a m] (conj a (zipmap (keys m) (map keyword (vals m))))) #{} tasks)]
    (assoc-in db [:wh.user.db/sub-db :wh.user.db/company :onboarding-tasks] tasks')))

(reg-event-fx
  :company/set-task-as-read
  (fn [{db :db} [_ task]]
    (when (s/valid? :wh.company.onboarding-task/id task)
      {:graphql {:query gql/set-task-as-read-mutation
                 :variables {:task (cases/->snake-case-str task)}
                 :on-success [::set-task-as-read-success]}})))

(reg-event-db
  ::set-task-as-read-success
  (fn [db [_ data]]
    (->onboarding-tasks db (get-in data [:data :setTaskAsRead :onboardingTasks]))))

(reg-event-fx
  :company/refresh-tasks
  (fn [{db :db} _]
    {:graphql {:query (gql/company-query
                        (get-in db [:wh.user.db/sub-db :wh.user.db/company-id])
                        [[:onboardingTasks [:id :state]]])
               :on-success [::refresh-tasks-success]}}))

(reg-event-db
  ::refresh-tasks-success
  (fn [db [_ data]]
    (->onboarding-tasks db (get-in data [:data :company :onboardingTasks]))))
