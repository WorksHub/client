(ns wh.admin.candidates.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [wh.admin.candidates.db :as candidates]
    [wh.components.pagination :as pagination]
    [wh.verticals :as verticals])
  (:require-macros [clojure.core.strint :refer [<<]]))

(reg-sub ::sub-db (fn [db _] (::candidates/sub-db db)))

(reg-sub
  ::search-term
  :<- [::sub-db]
  (fn [db _]
    (::candidates/search-term db)))

(reg-sub
  ::search-results
  :<- [::sub-db]
  (fn [db _]
    (::candidates/search-results db)))

(reg-sub
  ::facets-counts
  :<- [::sub-db]
  (fn [db _]
    (::candidates/facets-counts db)))

(reg-sub
  ::vertical-facets-counts
  :<- [::facets-counts]
  (fn [facets-counts _]
    (:board facets-counts)))

(reg-sub
  ::approval-facets-counts
  :<- [::facets-counts]
  (fn [facets-counts _]
    (:approval-status facets-counts)))

(reg-sub
  ::verticals
  :<- [::sub-db]
  (fn [db _]
    (::candidates/verticals db)))

(reg-sub
  ::verticals-options
  :<- [::sub-db]
  :<- [::vertical-facets-counts]
  (fn [[db facets] _]
    (mapv (fn [vertical]
            (let [count ((keyword vertical) facets)]
              {:option vertical
               :label  (str vertical (when count (str " (" count ")")))})) verticals/ordered-job-verticals)))

(reg-sub
  ::approvals-options
  :<- [::sub-db]
  :<- [::approval-facets-counts]
  (fn [[db facets] _]
    (mapv (fn [vertical]
            (let [count ((keyword vertical) facets)]
              {:option vertical
               :label  (str vertical (when count (str " (" count ")")))})) candidates/approval-statuses)))

(reg-sub
  ::approval-statuses
  :<- [::sub-db]
  (fn [db _]
    (::candidates/approval-statuses db)))

(reg-sub
  ::loading-state
  :<- [::sub-db]
  (fn [db _]
    (::candidates/loading-state db)))

(reg-sub
  ::pagination
  :<- [::sub-db]
  (fn [db _]
    (let [{:keys [current-page total-pages]} (::candidates/results-counts db)]
      (pagination/generate-pagination current-page total-pages))))

(reg-sub
  ::current-page
  :<- [::sub-db]
  (fn [db _]
    (-> db
        ::candidates/results-counts
        :current-page)))

(reg-sub
  ::query-params
  :<- [::sub-db]
  (fn [db _]
    (candidates/candidates-query-params db)))
