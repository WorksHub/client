(ns wh.issues.manage.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [wh.issues.manage.db :as manage]))

(reg-sub
  ::sub-db
  (fn [db _] (::manage/sub-db db)))

(reg-sub
  ::issues
  :<- [::sub-db]
  (fn [db [_ repo]]
    (filter #(= (:name repo) (get-in % [:repo :name]))
            (vals (::manage/issues db)))))

(reg-sub
  ::syncing-repos?
  :<- [::sub-db]
  (fn [db _]
    (::manage/syncing-repos? db)))

(reg-sub
  ::open-repo?
  :<- [::sub-db]
  (fn [db [_ repo]]
    (contains? (::manage/open-repos db) repo)))

(reg-sub
  ::fetched-repo?
  :<- [::sub-db]
  (fn [db [_ repo]]
    (contains? (::manage/fetched-repos db) (:name repo))))

(reg-sub
  ::syncing-issues?
  :<- [::sub-db]
  (fn [db [_ repo]]
    (contains? (::manage/syncing-issues db) (:name repo))))


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
