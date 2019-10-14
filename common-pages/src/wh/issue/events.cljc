(ns wh.issue.events
  (:require
    #?(:cljs [wh.pages.core :as pages :refer [on-page-load]])
    #?(:cljs [wh.user.db :as user])
    [re-frame.core :refer [path]]
    [wh.common.cases :as cases]
    [wh.common.issue :refer [gql-issue->issue]]
    [wh.common.job :refer [translate-job]]
    [wh.db :as db]
    [wh.graphql.issues :as queries]
    [wh.issue.db :as issue]
    [wh.re-frame.events :refer [reg-event-db reg-event-fx]]
    [wh.routes :as routes]
    [wh.util :as util]))

(def issue-interceptors (into db/default-interceptors
                              [(path ::issue/sub-db)]))

(reg-event-fx
  ::fetch-issue-success
  db/default-interceptors
  (fn [{db :db} [{{:keys [issue me]} :data}]]
    {:db (-> db
             (update ::issue/sub-db merge
                     {::issue/github-login (get-in me [:githubInfo :login])
                      ::issue/issue (gql-issue->issue issue)}))
     :dispatch-n (concat [[::fetch-company-issues]]
                         #?(:cljs
                            (when (db/logged-in? db)
                              [[::fetch-company-jobs]])))}))

(reg-event-db
  ::update-issue-success
  issue-interceptors
  (fn [db [issue]]
    (update db ::issue/issue merge issue)))

#?(:cljs
   (reg-event-fx
     ::fetch-issue
     db/default-interceptors
     (fn [{db :db} [id]]
       {:graphql {:query (if (db/logged-in? db)
                           queries/fetch-issue-and-login
                           queries/fetch-issue)
                  :variables {:id id}
                  :on-success [::fetch-issue-success]
                  :on-failure [:error/set-global "Something went wrong while we tried to fetch the issue’s data"
                               [::fetch-issue id]]}})))

#?(:cljs
   (reg-event-fx
     ::fetch-company-issues
     db/default-interceptors
     (fn [{db :db} _]
       {:graphql {:query queries/fetch-company-issues
                  :variables {:id (get-in db [::issue/sub-db ::issue/issue :company :id])
                              :published true
                              :page_size 3
                              :page_number 1}
                  :on-success [::fetch-company-issues-success]
                  :on-failure [:error/set-global "Something went wrong while we tried to fetch more of this company's issues 😢"
                               [::fetch-company-issues]]}})))

(reg-event-db
  ::fetch-company-issues-success
  db/default-interceptors
  (fn [db [{{:keys [query_issues]} :data}]]
    (assoc-in db
              [::issue/sub-db ::issue/company-issues]
              (->> (:issues query_issues)
                   (filter #(not= (:id %) (get-in db [::db/page-params :id]))) ;; remove _this_ issue
                   (map gql-issue->issue)))))

(reg-event-db
  ::set-show-cta-sticky?
  issue-interceptors
  (fn [db [show?]]
    (assoc db ::issue/show-cta-sticky? show?)))

(reg-event-fx
  ::show-start-work-popup
  issue-interceptors
  (fn [{db :db} [val]]
    {:db (assoc db ::issue/start-work-popup-shown? val)}))

#?(:cljs
   (reg-event-fx
     ::try-contribute
     db/default-interceptors
     (fn [{db :db} _]
       (if (db/logged-in? db)
         {:dispatch [::show-start-work-popup true]}
         {:show-auth-popup {:context :issue
                            :redirect [:issue
                                       :params {:id (get-in db [::issue/sub-db ::issue/issue :id])}]}}))))

#?(:cljs
   (reg-event-fx
     ::start-work
     issue-interceptors
     (fn [{db :db} _]
       (if (get-in db [::issue/issue :viewer-contributed])
         {:dispatch [::show-start-work-popup false]}
         {:db (assoc db ::issue/contribute-in-progress? true)
          :graphql {:query queries/start-work-mutation
                    :variables {:id (get-in db [::issue/issue :id])}
                    :on-success [::start-work-success]
                    :on-failure [::start-work-failure]}}))))

(defn me-as-contributor
  [db]
  (-> db
      (get-in [:wh.user.db/sub-db])
      (util/strip-ns-from-map-keys)
      (select-keys [:id :name :other-urls :github-info])))

(reg-event-fx
  ::start-work-success
  db/default-interceptors
  (fn [{db :db} _]
    {:db (-> db
             (assoc-in [::issue/sub-db ::issue/contribute-in-progress?] false)
             (assoc-in [::issue/sub-db ::issue/issue :viewer-contributed] true)
             (update-in [::issue/sub-db ::issue/issue :contributors] conj (me-as-contributor db)))
     :dispatch [::show-start-work-popup false]}))

(reg-event-fx
  ::start-work-failure
  issue-interceptors
  (fn [{db :db} _]
    {:db (assoc db ::issue/contribute-in-progress? false)
     :dispatch-n [[:error/set-global "We were unable to register that you're working on the issue"
                   [::show-start-work-popup true]]
                  [::show-start-work-popup false]]}))

(reg-event-fx
  ::initialize-db
  issue-interceptors
  (fn [{db :db} [val]]
    {:db (issue/default-db db)}))

(reg-event-db
  ::flush-issue
  issue-interceptors
  (fn [db _]
    (dissoc db ::issue/issue)))

#?(:cljs
   (reg-event-fx
     ::fetch-company-jobs
     db/default-interceptors
     (fn [{db :db} _]
       (if-let [id (get-in db [::issue/sub-db ::issue/issue :company :id])]
         {:graphql {:query      queries/fetch-company-jobs
                    :variables  {:id id}
                    :on-success [::fetch-company-jobs-success]
                    :on-failure [:error/set-global "Something went wrong while we tried to fetch the company's jobs"
                                 [::fetch-company-jobs]]}}
         {}))))

(reg-event-db
  ::fetch-company-jobs-success
  db/default-interceptors
  (fn [db [{{:keys [jobs]} :data}]]
    (assoc-in db
              [::issue/sub-db ::issue/company-jobs]
              (map translate-job jobs))))

#?(:cljs
   (defmethod on-page-load :issue [db]
     (let [old-id (get-in db [::issue/sub-db ::issue/issue :id])
           new-id (get-in db [::db/page-params :id])]
       (list
        [::initialize-db]
        (when (and (not (::db/initial-load? db))
                   (not= new-id old-id))
          [::flush-issue])
        (if-not (::db/initial-load? db)
          [::fetch-issue (get-in db [::db/page-params :id])]
          [::fetch-company-issues])
        (when (::db/initial-load? db)
          [::fetch-company-jobs])))))
