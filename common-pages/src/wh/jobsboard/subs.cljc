(ns wh.jobsboard.subs
  (:require [re-frame.core :refer [reg-sub]]
            [wh.job.db :as job-db]
            [wh.jobsboard.db :as jobsboard]
            [wh.verticals :as verticals]))

;; TODO: a lot of these may probably be shared with wh.jobs.jobsboard.subs

(reg-sub
  ::sub-db
  (fn [db _]
    (:wh.jobs.jobsboard.db/sub-db db)))

(reg-sub
  ::jobs
  :<- [::sub-db]
  (fn [sub-db _]
    (:wh.jobs.jobsboard.db/jobs sub-db)))

(reg-sub
  ::promoted-jobs
  :<- [::sub-db]
  (fn [sub-db _]
    (:wh.jobs.jobsboard.db/promoted-jobs sub-db)))

(reg-sub
  ::current-page
  :<- [::sub-db]
  (fn [sub-db _]
    (or (:wh.jobs.jobsboard.db/current-page sub-db) 1)))

(reg-sub
  ::total-pages
  :<- [::sub-db]
  (fn [sub-db _]
    (or (:wh.jobs.jobsboard.db/total-pages sub-db) 1)))

(reg-sub
  ::currencies
  :<- [::sub-db]
  (fn [sub-db _]
    (conj (get sub-db :wh.jobs.jobsboard.db/currencies []) "*")))

(reg-sub
  ::search-term
  :<- [::sub-db]
  (fn [sub-db _]
    (:wh.jobs.jobsboard.db/search-term-for-results sub-db)))

(reg-sub
  ::search-result-count
  :<- [::sub-db]
  (fn [sub-db _]
    (or (:wh.jobs.jobsboard.db/number-of-search-results sub-db) 0)))

(reg-sub
  ::search-label
  (fn [db _]
    (:wh.db/search-label db)))

(reg-sub
  ::result-count-str
  :<- [::search-result-count]
  :<- [::search-label]
  (fn [[count label] _]
    (let [label (if label (str "'" label "'") "your criteria")]
      (if (zero? count)
        (str "We found no jobs matching " label " \uD83D\uDE22")
        (str "We found " count " jobs matching " label)))))

(reg-sub
  ::header-info
  (fn [{:keys [wh.db/vertical :wh.db/search-label] :as db} _]
    (let [search-term (get-in db [:wh.db/page-params :tag])]
      (-> (verticals/config vertical :jobsboard-header)
          (update :title #(or search-label %))
          (update :description #(get (verticals/config vertical :jobsboard-tag-desc) search-term %))))))

(reg-sub
  ::view-type
  (fn [db _]
    (keyword (get-in db [:wh.db/query-params jobsboard/view-type-param] "cards"))))

(reg-sub
  ::role-types
  (fn [_ _]
    (for [role-type job-db/role-types]
      {:value role-type
       :label role-type})))

;; used to achieve parity with cljs version
(reg-sub
  ::side-jobs
  (fn [_ _] []))
