(ns wh.company.jobs.db
  (:require [wh.components.pagination :as pagination]))

(def page-size 12)

(def default-sort [{:field "last-modified"
                    :order :DESC}])

(defn company-slug [db]
  (get-in db [:wh.db/page-params :slug]))

(defn page-number [db]
  (pagination/qps->page-number (:wh.db/query-params db)))

(def default-unpublished? true)

(defn show-unpublished? [admin? company? unpublished?]
  (and (or admin? company?)
       (if (nil? unpublished?) default-unpublished? unpublished?)))

(defn published
  "Determines the value of the 'published' GraphQL query arg to pass.
   NB: It's `nil` for all (both published and unpublished) jobs OR
           `true` for published only.
       See `company-jobs` resolver and `fetch-jobs` for more details."
  [show-unpublished?]
  (when-not show-unpublished? true))
