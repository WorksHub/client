(ns wh.pages.issue.events
  (:require #?(:cljs [wh.pages.core :as pages :refer [on-page-load]])
            [re-frame.core :refer [path]]
            [wh.common.issue :refer [gql-issue->issue]]
            [wh.common.job :refer [translate-job]]
            [wh.common.keywords :as keywords]
            [wh.db :as db]
            [wh.graphql-cache :as cache]
            [wh.graphql.issues :as queries]
            [wh.pages.issue.db :as issue]
            [wh.re-frame.events :refer [reg-event-db reg-event-fx]]))

(def issue-interceptors (into db/default-interceptors
                              [(path ::issue/sub-db)]))

(defn initial-query [db]
  [(if (db/logged-in? db) :issue-and-me :issue) {:id (issue/id db)}])

(defn issue [db]
  (let [[query query-vars] (initial-query db)]
    (:issue (cache/result db query query-vars))))

(reg-event-fx
  ::update-issue-success
  db/default-interceptors
  (fn [{db :db} [issue]]
    {:dispatch (into [:graphql/update-entry]
                     (concat (initial-query db) [:merge {:issue issue}]))}))

(reg-event-fx
  ::fetch-company-issues
  db/default-interceptors
  (fn [{db :db} _]
    {:graphql {:query queries/fetch-issues
               :variables {:id (get-in (issue db) [:company :id])
                           :published true
                           :page_size 3
                           :page_number 1}
               :on-success [::fetch-company-issues-success]
               :on-failure [:error/set-global "Something went wrong while we tried to fetch more of this company's issues 😢"
                            [::fetch-company-issues]]}}))

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
                                       :params {:id (issue/id db)}]}}))))

#?(:cljs
   (reg-event-fx
     ::start-work
     db/default-interceptors
     (fn [{db :db} _]
       (if (:viewer-contributed (issue db))
         {:dispatch [::show-start-work-popup false]}
         {:db (assoc-in db [::issue/sub-db ::issue/contribute-in-progress?] true)
          :graphql {:query queries/start-work-mutation
                    :variables {:id (issue/id db)}
                    :on-success [::start-work-success]
                    :on-failure [::start-work-failure]}}))))

(defn me-as-contributor
  [db]
  (-> db
      (get-in [:wh.user.db/sub-db])
      (keywords/strip-ns-from-map-keys)
      (select-keys [:id :name :other-urls :github-info])))

(reg-event-fx
  ::start-work-success
  db/default-interceptors
  (fn [{db :db} _]
    (let [issue (-> db
                    issue
                    (assoc :viewer-contributed true)
                    (update :contributors conj (me-as-contributor db)))]
      {:db (assoc-in db [::issue/sub-db ::issue/contribute-in-progress?] false)
       :dispatch-n [[::show-start-work-popup false]
                    (into [:graphql/update-entry]
                          (concat (initial-query db) [:merge {:issue issue}]))]})))

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

#?(:cljs
   (reg-event-fx
     ::fetch-company-jobs
     db/default-interceptors
     (fn [{db :db} _]
       (if-let [id (get-in (issue db) [:company :id])]
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

(reg-event-fx
  ::on-issue-ready
  db/default-interceptors
  (fn [{db :db} _]
    {:dispatch-n [[::fetch-company-issues]
                  [::fetch-company-jobs]]
     :page-title {:page-name (issue/page-title (issue db))
                  :vertical  (:wh.db/vertical db)}}))

#?(:cljs
   (defmethod on-page-load :issue [db]
     (let [logged-in? (db/logged-in? db)
           initial-load? (::db/initial-load? db)]
       (list
         [::initialize-db]
         (if initial-load?
           [::on-issue-ready]
           (into [:graphql/query] (conj (initial-query db) {:on-complete [::on-issue-ready]})))))))
