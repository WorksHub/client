(ns wh.company.jobs.events
  (:require
    #?(:cljs [wh.pages.core :refer [on-page-load] :as pages])
    [wh.company.jobs.db :as jobs]))

(defn company-query [db]
  [:company-card {:slug (jobs/company-slug db)}])

(defn jobs-query [db]
  [:company-jobs-page {:slug        (jobs/company-slug db)
                       :page_size   jobs/page-size
                       :page_number (jobs/page-number db)
                       :sort        jobs/default-sort}])

#?(:cljs
   (defmethod on-page-load :company-jobs [db]
     (list (into [:graphql/query] (company-query db))
           (into [:graphql/query] (conj (jobs-query db)
                                        {:on-complete [:wh.events/scroll-to-top]})))))
