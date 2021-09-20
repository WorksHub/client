(ns wh.company.jobs.events
  (:require #?(:cljs [wh.pages.core :as pages :refer [on-page-load]])
            [re-frame.core :refer [path]]
            [wh.common.user :as user-common]
            [wh.company.jobs.db :as jobs]
            [wh.db :as db]
            [wh.re-frame.events :refer [reg-event-db reg-event-fx]]))

(def company-jobs-interceptors (into db/default-interceptors
                                     [(path ::jobs/sub-db)]))

(defn company-query [db]
  [:company-card {:slug (jobs/company-slug db)}])

(defn jobs-query [db]
  (let [show-unpublished? (jobs/show-unpublished?
                            (user-common/admin? db)
                            (user-common/company? db)
                            (get-in db [::jobs/sub-db ::jobs/unpublished?]))
        published         (jobs/published show-unpublished?)]
    [:company-jobs-page {:slug        (jobs/company-slug db)
                         :page_size   jobs/page-size
                         :page_number (jobs/page-number db)
                         :published   published
                         :sort        jobs/default-sort}]))

#?(:cljs
   (defmethod on-page-load :company-jobs [db]
     (list (into [:graphql/query] (company-query db))
           (into [:graphql/query] (conj (jobs-query db)
                                        {:on-complete [:wh.events/scroll-to-top]})))))

(reg-event-db
  ::initialize-db
  company-jobs-interceptors
  (fn [sub-db _]
    (assoc sub-db ::jobs/unpublished? jobs/default-unpublished?)))

(reg-event-fx
  ::toggle-unpublished
  company-jobs-interceptors
  (fn [{sub-db :db} _]
    {:dispatch [:wh.events/nav--query-params {"page" 1}]
     :db       (update sub-db ::jobs/unpublished? not)}))
