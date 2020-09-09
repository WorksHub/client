(ns wh.issues.subs
  (:require
    #?(:clj  [clj-time.coerce :as tc]
       :cljs [cljs-time.coerce :as tc])
    #?(:clj  [clj-time.format :as tf]
       :cljs [cljs-time.format :as tf])
    [clojure.string :as str]
    [re-frame.core :refer [reg-sub]]
    [wh.components.pagination :as pagination]
    [wh.issues.db :as issues]))

(reg-sub
  ::sub-db
  (fn [db _] (::issues/sub-db db)))

(reg-sub ::db (fn [db _] db))

(reg-sub
  ::sorting-by
  :<- [:wh/query-params]
  (fn [qps _]
    (keyword (issues/issues-sort qps))))

(reg-sub
  ::issues
  :<- [::sub-db]
  (fn [db _]
    (::issues/issues db)))

(reg-sub
  ::issues-count
  :<- [::sub-db]
  (fn [db _]
    (::issues/count db)))

(reg-sub
  ::sorting-options
  (fn [_ _]
    [{:id :published :label "Most Recent"}
     {:id :compensation :label "Reward"}]))

(reg-sub
  ::company
  :<- [::sub-db]
  (fn [db _]
    (::issues/company db)))

(reg-sub
  ::own-company?
  :<- [:wh/page]
  (fn [page _]
    (= :company-issues page)))

(reg-sub
  ::all-issues?
  (fn [db _]
    (and (not (contains? (:wh.db/page-params db) :company-id))
         (= :issues (:wh.db/page db)))))

(reg-sub
  ::issues-for-company-id?
  (fn [db _]
    (and (get-in db [:wh.db/page-params :company-id])
         (= :issues (:wh.db/page db)))))

(reg-sub
  ;; sub responsible for handling tagged :issues-for-company-id route
  ;; see: (bidi/tag :issues :issues-for-company-id) in wh.routes
  ;; without this sub, we wouldn't be able to create proper, paged routes for
  ;; Company Issues page
  ::page
  :<- [::issues-for-company-id?]
  :<- [:wh/page]
  (fn [[issues-for-company-id? page] _]
    (if issues-for-company-id?
      :issues-for-company-id
      page)))

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
  :<- [::db]
  (fn [[company db] _]
    (issues/title db (:name company))))

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
  (fn [[{:keys [::issues/company-id]} my-company] _]
    (and company-id
         (= (:id my-company) company-id))))

(reg-sub
 ::issues-languages
 :<- [::sub-db]
 (fn [db _]
   (::issues/languages db)))
