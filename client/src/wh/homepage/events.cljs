(ns wh.homepage.events
  (:require
    [ajax.formats :refer [text-request-format raw-response-format]]
    [cljs.reader :refer [read-string]]
    [re-frame.core :refer [reg-event-db reg-event-fx]]
    [wh.common.cases :as cases]
    [wh.common.job :as job]
    [wh.db :as db]
    [wh.graphql.jobs]
    [wh.homepage.db :as sub-db]
    [wh.pages.core :refer [on-page-load] :as pages]
    [wh.routes :as routes])
  (:require-macros
    [wh.graphql-macros :refer [defquery]]))

(defquery homepage-data-query
  {:venia/operation {:operation/type :query
                     :operation/name "homepage_data"}
   :venia/variables [{:variable/name "vertical"
                      :variable/type :vertical}]
   :venia/queries
   [[:blogs {:page_size 3 :page_number 1 :vertical :$vertical}
     [[:results
        [:id :title :feature :tags :author :formattedCreationDate :readingTime]]]]
    [:homepage_jobs {:vertical :$vertical}
     :fragment/jobCardFields]]})

(reg-event-fx
  ::fetch-initial-data
  db/default-interceptors
  (fn [{db :db} _]
    {:dispatch [::pages/set-loader]
     :graphql {:query      homepage-data-query
               :variables  {:vertical (::db/vertical db)}
               :on-success [::fetch-initial-data-success]
               :on-failure [::fetch-initial-data-failure]}}))

(reg-event-fx
  ::fetch-initial-data-success
  db/default-interceptors
  (fn [{db :db} [{{:keys [blogs homepage_jobs]} :data}]]
    {:dispatch [::pages/unset-loader]
     :db       (update db ::sub-db/sub-db merge
                       {::sub-db/blogs (mapv cases/->kebab-case (:results blogs))
                        ::sub-db/jobs  (mapv job/translate-job homepage_jobs)})}))

(reg-event-fx
  ::fetch-initial-data-failure
  db/default-interceptors
  (fn [_ _]
    {:dispatch [::pages/unset-loader]}))

(reg-event-fx
  ::login-as-success
  db/default-interceptors
  (fn [{db :db} [response]]
    (let [user-db (read-string response)]
      {:db (update db :wh.user.db/sub-db merge user-db)
       :dispatch [::pages/load-module-and-set-page {:handler :homepage}]})))

(reg-event-fx
  ::login-as
  db/default-interceptors
  (fn [{db :db} [email]]
    {:dispatch   [::pages/set-loader]
     :http-xhrio {:method           :post
                  :headers          {"Accept" "application/edn"}
                  :uri              (str (::db/api-server db)
                                         (routes/path :login-as :query-params {"email" email}))
                  :format           (text-request-format)
                  :response-format  (raw-response-format)
                  :with-credentials true
                  :timeout          10000
                  :on-success       [::login-as-success]}}))

(defmethod on-page-load :homepage-not-logged-in [db]
  [(when-not (get-in db [::sub-db/sub-db ::sub-db/jobs])
     [::fetch-initial-data])
   (when-let [email (get-in db [::db/query-params "login-as"])]
     [::login-as email])])
