(ns wh.company.jobs.db
  (:require
    [wh.components.pagination :as pagination]))

(def page-size 12)

(defn company-slug
  [db]
  (get-in db [:wh.db/page-params :slug]))

(defn page-number
  [db]
  (pagination/qps->page-number (:wh.db/query-params db)))
