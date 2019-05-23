(ns wh.issues.subs
  (:require
    #?(:clj [clj-time.coerce :as tc]
       :cljs [cljs-time.coerce :as tc])
    #?(:clj [clj-time.format :as tf]
       :cljs [cljs-time.format :as tf])
    [re-frame.core :refer [reg-sub]]
    [wh.common.issue :as common-issue]
    [wh.components.pagination :as pagination]
    [wh.issues.db :as issues]))

(reg-sub
  ::sub-db
  (fn [db _] (::issues/sub-db db)))

(reg-sub
  ::sorting-by
  :<- [::sub-db]
  (fn [db _]
    (::issues/sorting-by db)))

(reg-sub
  ::issues
  :<- [::sub-db]
  :<- [::sorting-by]
  (fn [[db sorting-by] _]
    (let [sort-fn (get common-issue/issue-sorting-fns
                       sorting-by
                       :created-at)]
      (sort-by sort-fn (vals (::issues/issues db))))))

(reg-sub
  ::issues-count
  :<- [::sub-db]
  (fn [db _]
    (::issues/count db)))

(reg-sub
  ::sorting-options
  (fn [_ _]
    [{:id :title      :label "Title"}
     {:id :repository :label "Repository"}
     {:id :created-at :label "Created At"}]))

(reg-sub
  ::company
  :<- [::sub-db]
  (fn [db _]
    (::issues/company db)))

(reg-sub
  ::page
  (fn [db _]
    (:wh.db/page db)))

(reg-sub
  ::own-company?
  :<- [::page]
  (fn [page _]
    (= :company-issues page)))

(reg-sub
  ::all-issues?
  (fn [db _]
    (and (not (contains? (:wh.db/page-params db) :company-id))
         (= :issues (:wh.db/page db)))))

(reg-sub
  ::issues-for-company-id
  (fn [db _]
    (and (get-in db [:wh.db/page-params :company-id])
         (= :issues (:wh.db/page db)))))

(reg-sub
  ::company-pod?
  :<- [::company]
  :<- [::own-company?]
  (fn [[company own-company?] _]
    (and company (not own-company?))))

(reg-sub
  ::github-orgs
  :<- [::issues]
  (fn [issues _]
    (distinct (map (comp :owner :repo) issues))))

(reg-sub
  ::header
  :<- [::company]
  (fn [company _]
    (str (:name company) " Open Source Issues")))

(reg-sub
  ::loading?
  :<- [::sub-db]
  (fn [db _]
    (::issues/loading? db)))

(reg-sub
  ::current-page-number
  :<- [::sub-db]
  (fn [db _]
    (::issues/current-page-number db)))

(reg-sub
  ::page-size
  :<- [::sub-db]
  (fn [db _]
    (::issues/page-size db)))

(reg-sub
  ::total-pages
  :<- [::sub-db]
  (fn [db _]
    (::issues/total-pages db)))

(reg-sub
  ::issues-count-str
  :<- [::issues-count]
  :<- [::page-size]
  :<- [::current-page-number]
  (fn [[issues-count page-size current-page-number] _]
    (when (and issues-count page-size current-page-number)
      (let [start (inc (* page-size (dec current-page-number)))
            end (min (* page-size current-page-number) issues-count)]
        (if (< issues-count page-size)
          (str "Showing " 1 "-" issues-count " of " issues-count " issues")
          (str "Showing " start "-" end " of " issues-count " issues"))))))

(reg-sub
  ::query-params
  (fn [db _]
    (:wh.db/query-params db)))

(reg-sub
  ::page-params
  (fn [db _]
    (:wh.db/page-params db)))

(reg-sub
  ::pagination
  :<- [::current-page-number]
  :<- [::total-pages]
  (fn [[current-page-number total-pages] _]
    (pagination/generate-pagination current-page-number total-pages)))

(reg-sub
  ::show-webhook-info?
  :<- [::sub-db]
  (fn [db _]
    (::issues/show-webhook-info? db)))

(reg-sub
  ::company-view?
  (fn [db _]
    (boolean (get-in db [:wh.db/page-params :company-id]))))

(reg-sub
  ::jobs
  :<- [::sub-db]
  (fn [db _]
    (::issues/jobs db)))

(reg-sub
  ::has-jobs?
  :<- [::jobs]
  (fn [jobs _]
    (not (nil? jobs))))

(reg-sub
  ::can-manage-issues?
  :<- [::sub-db]
  :<- [:wh.user.subs/company] ;; TODO we shouldn't include subs from another place
  :<- [:wh.user.subs/workshub?]
  (fn [[{:keys [::issues/company-id]} my-company workshub?] _]
    (and company-id
         workshub?
         (= (:id my-company) company-id))))
