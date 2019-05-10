(ns wh.issue.edit.events
  (:require
    #?(:cljs [wh.pages.core :as pages :refer [on-page-load]])
    [re-frame.core :refer [path]]
    [wh.common.issue :refer [gql-issue->issue]]
    [wh.db :as db]
    [wh.graphql.issues :as queries]
    [wh.issue.edit.db :as issue-edit]
    [wh.re-frame.events :refer [reg-event-db reg-event-fx]]))

(def edit-issue-interceptors (into db/default-interceptors
                                   [(path ::issue-edit/sub-db)]))

(reg-event-db
  ::initialize-db
  edit-issue-interceptors
  (fn [_ _]
    issue-edit/default-db))

(reg-event-db
  :edit-issue/show-issue-edit-popup
  edit-issue-interceptors
  (fn [db [issue on-success]]
    (-> db
        (assoc ::issue-edit/displayed-dialog :edit)
        (assoc ::issue-edit/on-success on-success)
        (assoc ::issue-edit/current-issue issue))))

(reg-event-db
  ::close-issue-edit-popup
  edit-issue-interceptors
  (fn [db _]
    (-> db
        (assoc ::issue-edit/displayed-dialog nil
               ::issue-edit/pending-level nil
               ::issue-edit/pending-status nil
               ::issue-edit/pending-compensation nil)
        (dissoc ::issue-edit/current-issue))))

(reg-event-db
  ::set-pending-level
  edit-issue-interceptors
  (fn [db [level]]
    (assoc db ::issue-edit/pending-level level)))

(reg-event-db
  ::set-pending-status
  edit-issue-interceptors
  (fn [db [status]]
    (assoc db ::issue-edit/pending-status status)))

(reg-event-db
  ::set-pending-compensation
  edit-issue-interceptors
  (fn [db [compensation]]
    (assoc db ::issue-edit/pending-compensation compensation)))

#?(:cljs
   (reg-event-fx
     ::save-issue
     edit-issue-interceptors
     (fn [{{:keys [::issue-edit/pending-level
                   ::issue-edit/pending-status
                   ::issue-edit/pending-compensation] :as db} :db}
          [confirmed?]]
       (let [current-status (get-in db [::issue-edit/current-issue :status])
             new-status (or pending-status current-status)]
         (if-not (or confirmed? (= current-status new-status))
           {:db      (assoc db ::issue-edit/displayed-dialog :confirm)}
           {:db      (assoc db ::issue-edit/updating? true)
            :graphql {:query      queries/update-issue-mutation
                      :variables  {:id           (get-in db [::issue-edit/current-issue :id])
                                   :compensation {:amount (or pending-compensation (get-in db [::issue-edit/current-issue :compensation :amount]) 0)
                                                  :currency :EUR}
                                   :status       new-status
                                   :level        (or pending-level (get-in db [::issue-edit/current-issue :level]))}
                      :on-success [::update-issue-success]
                      :on-failure [::save-issue-failure]}})))))

#?(:cljs
   (reg-event-fx
     ::update-issue-success
     edit-issue-interceptors
     (fn [{db :db} [{{:keys [issue]} :data}]]
       {:db         (assoc db ::issue-edit/updating? false)
        :dispatch-n [(when-let [on-success (::issue-edit/on-success db)]
                       (conj on-success (gql-issue->issue issue)))
                     [::close-issue-edit-popup]]})))

(reg-event-fx
  ::save-issue-failure
  edit-issue-interceptors
  (fn [{db :db} _]
    {:db       (assoc db ::issue-edit/updating? false)
     :dispatch [:error/set-global "Something went wrong while we tried to update the issue"
                [::save-issue]]}))
