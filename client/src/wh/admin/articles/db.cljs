(ns wh.admin.articles.db
  (:require [wh.components.pagination :as pagination]))

(def page-size 20)

(defn page-number
  [db]
  (pagination/qps->page-number (:wh.db/query-params db)))

(defn initialize-db
  [db]
  (merge {::articles     nil
          ::pagination   nil
          ::in-progress? false
          ::error?       false} db))
