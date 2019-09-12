(ns wh.issues.manage.events
  (:require
    #?(:cljs [wh.pages.core :as pages :refer [on-page-load]])
    [#?(:cljs cljs-time.core
        :clj clj-time.core) :as t]
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

(defn page
  [db]
  (get-in db [::db/query-params "page"] 1))

(reg-event-db
  ::initialize-db
  manage-issues-interceptors
  (fn [db _]
    (manage/default-db db)))

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
       {:navigate [:manage-issues]})))

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
    (cond-> (-> db
                (assoc-in [::manage/sub-db ::manage/syncing-issues] false)
                (assoc-in [::manage/sub-db ::manage/loading?] false))
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
       {:db      (assoc-in db [::manage/sub-db ::manage/loading?] true)
        :graphql {:query      graphql-issues/fetch-company-issues--logged-in
                  :variables  {:id company-id
                               :page_size      default-page-size
                               :page_number    (or page-number 1)
                               :repo_owner (:owner repo)
                               :repo_name (:name repo)}
                  :on-success [::query-issues-success repo]
                  :on-failure [::failure [::query-issues company-id repo page-number]]}})))

(defn poll-event-dispatch
  [repo sync]
  {:id :poll-for-sync-progress
   :dispatch [::poll-sync-progress repo sync]
   :timeout 1000})

(reg-event-fx
  ::poll-sync-progress-success
  db/default-interceptors
  (fn [{db :db} [repo sync resp]]
    (let [new-sync (some-> (get-in resp [:data :repo :sync])
                           (cases/->kebab-case))]
      (if (:time-finished new-sync)
        {:db (-> db (update-in [::manage/sub-db ::manage/repo-syncs {:owner (:owner repo)
                                                                     :name (:name repo)}]
                               merge {:running-issue-count (:total-issue-count sync)
                                      :time-finished (:time-finished new-sync)})
                 (update-in [::manage/sub-db ::manage/fetched-repos] (fnil conj #{}) (:name repo)))
         :dispatch [::query-issues (get-in db [:wh.user.db/sub-db :wh.user.db/company-id]) repo (page db)]}
        {:db (update-in db [::manage/sub-db ::manage/repo-syncs {:owner (:owner repo)
                                                                 :name (:name repo)}]
                        merge (util/remove-nils new-sync) {:time-checked (str (t/now))})
         :dispatch-debounce (poll-event-dispatch repo sync)}))))

(reg-event-fx
  ::poll-sync-progress
  db/default-interceptors
  (fn [{db :db} [repo sync]]
    {:graphql {:query      graphql-issues/fetch-repo-query
               :variables  {:owner (:owner repo) :name (:name repo)}
               :on-success [::poll-sync-progress-success repo sync]
               :on-failure [::failure [::poll-sync-progress repo sync]]}}))


(reg-event-fx
  ::sync-repo-success
  db/default-interceptors
  (fn [{db :db} [repo resp]]
    (let [sync (cases/->kebab-case (get-in resp [:data :sync]))
          updated-db (-> db
                         (assoc-in [::manage/sub-db ::manage/repo-syncs (select-keys repo [:name :owner])] sync))
          sync-required? (not (:time-finished sync))]
      (if sync-required?
        {:db updated-db
         :dispatch-debounce (poll-event-dispatch repo sync)}
        {:db (assoc-in updated-db [::manage/sub-db ::manage/syncing-issues] false)
         :dispatch [::query-issues (get-in db [:wh.user.db/sub-db :wh.user.db/company-id]) repo (page db)]}))))

#?(:cljs
   (reg-event-fx
     ::sync-repo-issues
     db/default-interceptors
     (fn [{db :db} [{:keys [name owner] :as repo} force?]]
       {:db      (assoc-in db [::manage/sub-db ::manage/syncing-issues] true)
        :graphql {:query      graphql-company/sync-issues-mutation
                  :variables  (merge {:name  name
                                      :owner owner}
                                     (when force?
                                       {:force true}))
                  :on-success [::sync-repo-success repo]
                  :on-failure [::failure [::sync-repo-issues repo]]}})))

(reg-event-fx
  ::fetch-repos-success
  manage-issues-interceptors
  (fn [{db :db} [{data :data}]]
    (let [{:keys [github-repositories]} (cases/->kebab-case data)]
      {:db (merge db {::manage/repos          (:repositories github-repositories)
                      ::manage/syncing-repos? false})})))

#?(:cljs
   (reg-event-fx
     ::fetch-repos
     db/default-interceptors
     (fn [{db :db} _]
       {:db      (assoc-in db [::manage/sub-db ::manage/syncing-repos?] true)
        :graphql {:query      graphql-company/sync-repos
                  :on-success [::fetch-repos-success]
                  :on-failure [::failure [::fetch-repos]]}})))

#?(:cljs
   (reg-event-fx
     ::connect-github-app-failure
     db/default-interceptors
     (fn [{db :db} _]
       {:db (assoc-in db [::manage/sub-db ::manage/connect-github-app-error?] true)})))

#?(:cljs
   (defmethod on-page-load :manage-issues [db]
     (let [error (get-in db [::db/query-params "error"])]
       (list [:wh.events/scroll-to-top]
             [::initialize-db]
             (if error
               [::connect-github-app-failure]
               [::fetch-repos])))))

#?(:cljs
   (defmethod on-page-load :manage-repository-issues [db]
     (let [repo {:name (get-in db [::db/page-params :repo-name]) :owner (get-in db [::db/page-params :owner])}
           sync? (nil? (get-in db [::manage/sub-db ::manage/repo-syncs repo]))]
       (list [:wh.events/scroll-to-top]
             [::initialize-db]
             (if sync?
               [::sync-repo-issues repo]
               [::query-issues (get-in db [:wh.user.db/sub-db :wh.user.db/company-id]) repo (page db)])))))
