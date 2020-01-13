(ns wh.issues.manage.subs
  (:require
    [#?(:cljs cljs-time.coerce
        :clj clj-time.coerce) :as tc]
    [#?(:cljs cljs-time.core
        :clj clj-time.core) :as t]
    [#?(:cljs cljs-time.format
        :clj clj-time.format) :as tf]
    [re-frame.core :refer [reg-sub]]
    [wh.components.pagination :as pagination]
    [wh.db :as db]
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
  ::connect-github-app-error?
  :<- [::sub-db]
  (fn [db _]
    (::manage/connect-github-app-error? db)))

(reg-sub
  ::fetched-repo?
  :<- [::sub-db]
  (fn [db [_ repo]]
    (contains? (::manage/fetched-repos db) (:name repo))))

(reg-sub
  ::syncing-issues?
  :<- [::sub-db]
  (fn [db [_]]
    (::manage/syncing-issues db)))

(reg-sub
  ::repos
  :<- [::sub-db]
  (fn [db _]
    (::manage/repos db)))

(reg-sub
  ::number-of-published-issues
  :<- [::sub-db]
  (fn [db _]
    (::manage/number-of-published-issues db)))

(reg-sub
  ::open-issues-on-all-repos
  :<- [::repos]
  (fn [repos _]
    (->> repos
         (map :open-issues-count)
         (reduce +))))

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
  ::repo-name
  (fn [db _]
    (get-in db [:wh.db/page-params :repo-name])))

(reg-sub
  ::repo-owner
  (fn [db _]
    (get-in db [:wh.db/page-params :owner])))

(reg-sub
  ::repo
  (fn [db _]
    {:name (get-in db [::db/page-params :repo-name])
     :owner (get-in db [::db/page-params :owner])}))

(reg-sub
  ::full-repo-name
  :<- [::repo-owner]
  :<- [::repo-name]
  (fn [[repo-owner repo-name] _]
    (str repo-owner "/" repo-name)))

(reg-sub
  ::repo-sync
  :<- [::sub-db]
  (fn [db [_ repo-owner repo-name]]
    (get-in db [::manage/repo-syncs {:owner repo-owner :name repo-name}])))

(reg-sub
  ::current-repo-sync
  :<- [::sub-db]
  :<- [::repo-owner]
  :<- [::repo-name]
  (fn [[db repo-owner repo-name] _]
    (get-in db [::manage/repo-syncs {:owner repo-owner :name repo-name}])))

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

(reg-sub
  ::issues-loading?
  :<- [::sub-db]
  (fn [db _]
    (::manage/loading? db)))

(def minutes-until-sync-stall 3)

(reg-sub
  ::sync-wants-restart?
  :<- [::current-repo-sync]
  (fn [sync _]
    (when-let [tu (some->> (:time-updated sync) (tf/parse (tf/formatters :date-time)))]
      (< minutes-until-sync-stall (t/in-minutes (t/interval tu (t/now)))))))

(reg-sub
  ::slack-connected?
  :<- [::company]
  (fn [company [_]]
    (when company
      (get-in company [:integrations :slack :enabled] false))))
