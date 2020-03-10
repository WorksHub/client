(ns wh.company.articles.events
  (:require
    #?(:cljs [wh.pages.core :refer [on-page-load] :as pages])
    [wh.company.articles.db :as articles]
    [wh.components.pagination :as pagination]))

(defn company-query [db]
  [:company-card {:slug (articles/company-slug db)}])

(defn articles-query [db]
  [:company-articles-page (articles/params db)])

#?(:cljs
   (defmethod on-page-load :company-articles [db]
     (list (into [:graphql/query] (company-query db))
           (into [:graphql/query] (conj (articles-query db)
                                        {:on-complete [:wh.events/scroll-to-top]})))))
