(ns wh.issues.manage.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [wh.components.pagination :as pagination]
    [wh.issues.manage.db :as manage]))

(reg-sub
  ::sub-db
  (fn [db _] (::manage/sub-db db)))

(reg-sub
  ::issues
  :<- [::sub-db]
  (fn [db _]
    (vals (::manage/issues db))))

(reg-sub
  ::syncing-repos?
  :<- [::sub-db]
  (fn [db _]
    (::manage/syncing-repos? db)))

(reg-sub
  ::fetched-repo?
  :<- [::sub-db]
  (fn [db [_ repo]]
    (contains? (::manage/fetched-repos db) (:name repo))))

(reg-sub
  ::syncing-issues?
  :<- [::sub-db]
  (fn [db [_ ]]
    (::manage/syncing-issues db)))

(reg-sub
  ::orgs
  :<- [::sub-db]
  (fn [db _]
    (::manage/orgs db)))

(reg-sub
  ::update-disabled?
  :<- [::sub-db]
  (fn [db _]
    (empty? (::manage/pending db))))

(reg-sub
  ::company
  :<- [::sub-db]
  (fn [db _]
    (::manage/company db)))
(reg-sub
  ::full-repo-name
  (fn [db _]
    (str (get-in db [:wh.db/page-params :owner]) "/" (get-in db [:wh.db/page-params :repo-name]))))

(reg-sub
  ::current-page-number
  :<- [::sub-db]
  (fn [db _]
    (::manage/current-page-number db)))

(reg-sub
  ::page-size
  :<- [::sub-db]
  (fn [db _]
    (::manage/page-size db)))

(reg-sub
  ::total-pages
  :<- [::sub-db]
  (fn [db _]
    (::manage/total-pages db)))

(reg-sub
  ::pagination
  :<- [::current-page-number]
  :<- [::total-pages]
  (fn [[current-page-number total-pages] _]
    (pagination/generate-pagination current-page-number total-pages)))