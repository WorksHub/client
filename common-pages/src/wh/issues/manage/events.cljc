(ns wh.issues.manage.events
  (:require
    #?(:cljs [wh.pages.core :as pages :refer [on-page-load]])
    [re-frame.core :refer [path]]
    [wh.common.cases :as cases]
    [wh.common.issue :refer [gql-issue->issue]]
    [wh.db :as db]
    [wh.graphql.company :as graphql-company]
    [wh.graphql.issues :as graphql-issues]
    [wh.issues.manage.db :as manage]
    [wh.re-frame.events :refer [reg-event-db reg-event-fx]]
    [wh.util :as util]))

(def manage-issues-interceptors (into db/default-interceptors
                                      [(path ::manage/sub-db)]))

(reg-event-db
  ::initialize-db
  manage-issues-interceptors
  (fn [_ _]
    manage/default-db))

(reg-event-fx
  ::failure
  manage-issues-interceptors
  (fn [_ [retry-fn _error]]
    {:dispatch [:error/set-global "Something went wrong while we tried to fetch your data 😢"
                [retry-fn]]}))


(reg-event-db
  ::update-pending
  manage-issues-interceptors
  (fn [db [issues published?]]
    (reduce (fn [acc issue]
              (if (= published? (get-in acc [::manage/issues (:id issue) :published]))
                acc
                (-> acc
                    (assoc-in [::manage/issues (:id issue) :published] published?)
                    (update-in [::manage/pending] util/toggle (:id issue)))))
            db
            issues)))

(reg-event-fx
  ::toggle-repo
  db/default-interceptors
  (fn [{db :db} [repo]]
    (let [need-to-fetch-issues? (not (contains? (get-in db [::manage/sub-db ::manage/fetched-repos])
                                                (:name repo)))]
      (cond-> {:db (update-in db [::manage/sub-db ::manage/open-repos] util/toggle repo)}
        need-to-fetch-issues? (merge {:dispatch [::fetch-repo-issues repo]})))))

#?(:cljs
   (reg-event-fx
     ::update-issues-success
     db/default-interceptors
     (fn [_ _]
       {:navigate [:company-issues]})))

(reg-event-fx
  ::save-changes
  manage-issues-interceptors
  (fn [{db :db} _]
    {:db      (assoc db ::manage/updating? true)
     :graphql {:query      graphql-issues/update-issues-mutation
               :variables  {:issues (map #(select-keys % [:id :published])
                                         (-> (::manage/issues db)
                                             (select-keys (::manage/pending db))
                                             (vals)))}
               :on-success [::update-issues-success]
               :on-failure [::failure ::update-issues-success]}}))

(reg-event-db
  ::query-issues-success
  db/default-interceptors
  (fn [db [repo {{:keys [query_issues company me]} :data}]]
    (cond-> (update-in db [::manage/sub-db ::manage/syncing-issues] disj (:name repo))
      query_issues (update ::manage/sub-db merge
                           {::manage/issues   (into {} (->> (map (comp (juxt :id identity)
                                                                       gql-issue->issue)
                                                                 (:issues query_issues))))})

      company (update ::manage/sub-db merge {::manage/company (into {} company)})
      me (update :wh.user.db/sub-db merge
                 {:wh.user.db/welcome-msgs              (set (:welcomeMsgs me))
                  :wh.user.db/company-connected-github? (get-in me [:company :connectedGithub])}))))

#?(:cljs
   (reg-event-fx
     ::query-issues
     db/default-interceptors
     (fn [{db :db} [company-id repo]]
       {:graphql {:query      graphql-issues/fetch-company-issues--logged-in
                  :variables  {:id company-id}
                  :on-success [::query-issues-success repo]
                  :on-failure [::failure ::query-issues-success]}})))

(reg-event-fx
  ::fetch-repo-success
  db/default-interceptors
  (fn [{db :db} [repo]]
    {:db       (-> db
                   (update-in [::manage/sub-db ::manage/fetched-repos] (fnil conj #{}) (:name repo)))
     :dispatch [::query-issues (get-in db [:wh.user.db/sub-db :wh.user.db/company-id]) repo]}))

#?(:cljs
   (reg-event-fx
     ::fetch-repo-issues
     db/default-interceptors
     (fn [{db :db} [{:keys [name owner] :as repo}]]
       {:db      (update-in db [::manage/sub-db ::manage/syncing-issues] (fnil conj #{}) (:name repo))
        :graphql {:query      graphql-company/sync-issues-mutation
                  :variables  {:name  name
                               :owner owner}
                  :on-success [::fetch-repo-success repo]
                  :on-failure [::failure [::fetch-repo-success]]}})))

(reg-event-fx
  ::fetch-orgs-success
  manage-issues-interceptors
  (fn [{db :db} [{data :data}]]
    (let [{:keys [github-organisations]} (cases/->kebab-case data)]
      {:db (merge db {::manage/orgs           (:organisations github-organisations)
                      ::manage/syncing-repos? false})})))


#?(:cljs
   (reg-event-fx
     ::fetch-orgs-and-repos
     db/default-interceptors
     (fn [{db :db} _]
       {:db      (assoc-in db [::manage/sub-db ::manage/syncing-repos?] true)
        :graphql {:query      graphql-company/sync-orgs-and-repos
                  :on-success [::fetch-orgs-success]
                  :on-failure [::failure [::fetch-orgs-success]]}})))

#?(:cljs
   (defmethod on-page-load :manage-issues [_db]
     [[:wh.events/scroll-to-top]
      [::initialize-db]
      [::fetch-orgs-and-repos]]))
