(ns wh.issues.manage.events
  (:require
    #?(:cljs [wh.pages.core :as pages :refer [on-page-load]])
    [re-frame.core :refer [path]]
    [wh.common.cases :as cases]
    [wh.common.issue :refer [gql-issue->issue]]
    [wh.components.pagination :as pagination]
    [wh.db :as db]
    [wh.graphql.company :as graphql-company]
    [wh.graphql.issues :as graphql-issues]
    [wh.issues.manage.db :as manage]
    [wh.re-frame.events :refer [reg-event-db reg-event-fx]]
    [wh.util :as util]))

(def manage-issues-interceptors (into db/default-interceptors
                                      [(path ::manage/sub-db)]))

(def default-page-size 20)

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
                retry-fn]}))


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
               :on-failure [::failure [::save-changes]]}}))

(reg-event-db
  ::query-issues-success
  db/default-interceptors
  (fn [db [repo {{:keys [query_issues company me]} :data}]]
    (cond-> (assoc-in db [::manage/sub-db ::manage/syncing-issues] false)
      query_issues (update ::manage/sub-db merge
                           {::manage/issues   (into {} (->> (map (comp (juxt :id identity)
                                                                       gql-issue->issue)
                                                                 (:issues query_issues))))
                            ::manage/page-size           default-page-size
                            ::manage/count               (get-in query_issues [:pagination :total])
                            ::manage/current-page-number (get-in query_issues [:pagination :page_number])
                            ::manage/total-pages         (pagination/number-of-pages (get-in query_issues [:pagination :page_size])
                                                                              (get-in query_issues [:pagination :total]))})

      company (update ::manage/sub-db merge {::manage/company (into {} company)})
      me (update :wh.user.db/sub-db merge
                 {:wh.user.db/welcome-msgs              (set (:welcomeMsgs me))}))))

#?(:cljs
   (reg-event-fx
     ::query-issues
     db/default-interceptors
     (fn [{:keys [db]} [company-id repo page-number]]
       {:db      db
        :graphql {:query      graphql-issues/fetch-company-issues--logged-in
                  :variables  {:id company-id
                               :page_size      default-page-size
                               :page_number    (or page-number 1)}
                  :on-success [::query-issues-success repo]
                  :on-failure [::failure [::query-issues company-id repo page-number]]}})))

(reg-event-fx
  ::fetch-repo-success
  db/default-interceptors
  (fn [{db :db} [repo]]
    {:db       (-> db
                   (update-in [::manage/sub-db ::manage/fetched-repos] (fnil conj #{}) (:name repo)))
     :dispatch [::query-issues (get-in db [:wh.user.db/sub-db :wh.user.db/company-id]) repo (get-in db [::db/query-params "page"])]}))

#?(:cljs
   (reg-event-fx
     ::fetch-repo-issues
     db/default-interceptors
     (fn [{db :db} [{:keys [name owner] :as repo}]]
       {:db      (assoc-in db [::manage/sub-db ::manage/syncing-issues] true)
        :graphql {:query      graphql-company/sync-issues-mutation
                  :variables  {:name  name
                               :owner owner}
                  :on-success [::fetch-repo-success repo]
                  :on-failure [::failure [::fetch-repo-issues repo]]}})))

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
                  :on-failure [::failure [::fetch-orgs-and-repos]]}})))

#?(:cljs
   (defmethod on-page-load :manage-issues [_db]
     [[:wh.events/scroll-to-top]
      [::initialize-db]
      [::fetch-orgs-and-repos]]))

#?(:cljs
   (defmethod on-page-load :manage-repository-issues [db]
     [[:wh.events/scroll-to-top]
      [::initialize-db]
      [::fetch-repo-issues {:name (get-in db [::db/page-params :repo-name])
                            :owner (get-in db [::db/page-params :owner])}]]))
