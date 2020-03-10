(ns wh.company.articles.db
  (:require
    [wh.components.pagination :as pagination]))

(def page-size 12)

(defn company-slug
  [db]
  (get-in db [:wh.db/page-params :slug]))

(defn page-number
  [db]
  (pagination/qps->page-number (:wh.db/query-params db)))

(defn params
  [db]
  {:slug        (company-slug db)
   :page_size   page-size
   :page_number (page-number db)})
