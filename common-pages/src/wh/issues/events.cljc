(ns wh.issues.events
  (:require #?(:cljs [wh.pages.core :as pages :refer [on-page-load]])
            [re-frame.core :refer [path]]
            [wh.common.cases :as cases]
            [wh.common.issue :refer [gql-issue->issue]]
            [wh.db :as db]
            [wh.graphql.issues :as queries]
            [wh.issues.db :as issues]
            [wh.re-frame.events :refer [reg-event-db reg-event-fx]]
            [wh.slug :as slug]
            [wh.util :as util]))

(def issues-interceptors (into db/default-interceptors
                               [(path ::issues/sub-db)]))

(reg-event-db
  ::initialize-db
  issues-interceptors
  (fn [_ _]
    issues/default-db))

(reg-event-fx
  ::failure
  issues-interceptors
  (fn [_ [retry-fn _error]]
    {:dispatch [:error/set-global
                "Something went wrong while we tried to fetch your data 😢"
                [retry-fn]]}))

(reg-event-db
  ::fetch-issues-success
  db/default-interceptors
  (fn [db [{:keys [data]}]]
    (issues/update-issues-db db data)))

#?(:cljs
   (defn fetch-issues-query
     [db company-id]
     (if company-id
       (if (db/logged-in? db)
         queries/fetch-company-issues--logged-in
         queries/fetch-company-issues--not-logged-in)
       queries/fetch-issues)))

#?(:cljs
   (reg-event-fx
    ::fetch-issues-languages
    db/default-interceptors
    (fn [{db :db} []]
      {:graphql {:query      queries/fetch-issues-languages
                 :on-success [::fetch-issues-languages-success]
                 :on-failure [::failure ::fetch-issues-languages]}})))

#?(:cljs
   (reg-event-fx
     ::fetch-issues
     db/default-interceptors
     (fn [{db :db} [company-id page-number language]]
       {:db      (update db ::issues/sub-db merge
                         {::issues/loading?   true
                          ::issues/company-id company-id})
        :graphql {:query      (fetch-issues-query db company-id)
                  :variables  {:id            company-id
                               :page_size     issues/default-page-size
                               :published     true
                               :repo_language language
                               :page_number   (util/coerce-int (or page-number 1))
                               :sort          (issues/issues-sort
                                                (::db/query-params db))}
                  :on-success [::fetch-issues-success]
                  :on-failure [::failure ::fetch-issues]}})))

#?(:cljs
   (reg-event-fx
     ::fetch-jobs
     db/default-interceptors
     (fn [{db :db} [company-id]]
       {:graphql {:query      queries/fetch-company-jobs--lite
                  :variables  {:id          company-id
                               :page_size   3
                               :page_number 1}
                  :on-success [::fetch-jobs-success]
                  :on-failure [:error/set-global "Something went wrong while we tried to fetch the jobs for this company "]}})))

#?(:cljs
   (reg-event-db
    ::fetch-issues-languages-success
    db/default-interceptors
    (fn [db [resp]]
      (let [languages (get-in resp [:data :query_issues_languages :issues_languages])]
        (assoc-in db [::issues/sub-db ::issues/languages] languages)))))

#?(:cljs
   (reg-event-db
     ::fetch-jobs-success
     db/default-interceptors
     (fn [db [resp]]
       (assoc-in db [::issues/sub-db ::issues/jobs]
                 (cases/->kebab-case (get-in resp [:data :jobs]))))))

(reg-event-db
  :issues/show-webhook-info
  issues-interceptors
  (fn [db [show?]]
    (assoc db ::issues/show-webhook-info? show?)))

(defn update-issue [issue-to-update issue]
  (if (= (:id issue-to-update) (:id issue))
    (merge issue (gql-issue->issue issue-to-update))
    issue))

(reg-event-fx
  ::set-page-title
  db/default-interceptors
  (fn [{db :db} _]
    {:page-title {:page-name (issues/title db nil)
                  :vertical (:wh.db/vertical db)}}))

#?(:cljs
   (reg-event-db
     ::update-issue-success
     db/default-interceptors
     (fn [db [issue]]
       (update-in db [::issues/sub-db ::issues/issues]
                  #(map (partial update-issue issue) %)))))

#?(:cljs
   (do
     (defn on-issues-load [db]
       (let [company-id      (get-in db [::db/page-params :company-id])
             page-number     (get-in db [::db/query-params "page"])
             issues-language (issues/language db)]

         (concat (if (::db/initial-load? db)
                   []
                   [[::initialize-db]
                    [:wh.events/scroll-to-top]
                    [::set-page-title]
                    [::fetch-issues-languages]
                    [::fetch-issues company-id page-number
                     (some-> issues-language slug/slug->label)]])
                 (when company-id
                   [[::fetch-jobs company-id]]))))

     (defmethod on-page-load :issues [db]
       (on-issues-load db))

     (defmethod on-page-load :issues-by-language [db]
       (on-issues-load db))))

#?(:cljs
   (defmethod on-page-load :company-issues [db]
     (let [company-id (get-in db [:wh.user.db/sub-db :wh.user.db/company-id])
           page-number (get-in db [::db/query-params "page"])]
       [[::initialize-db]
        [:wh.events/scroll-to-top]
        [::set-page-title]
        [::fetch-issues company-id page-number]])))
