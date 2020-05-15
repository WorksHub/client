(ns wh.admin.articles.subs
  (:require [re-frame.core :refer [reg-sub]]
            [wh.admin.articles.db :as articles]
            [wh.components.pagination :as pagination]))

(reg-sub
  ::sub-db
  (fn [db _] (::articles/sub-db db)))

(reg-sub
  ::articles
  :<- [::sub-db]
  (fn [sub-db _]
    (::articles/articles sub-db)))

(reg-sub
  ::error?
  :<- [::sub-db]
  (fn [sub-db _]
    (::articles/error? sub-db)))

(reg-sub
  ::current-page-number
  (fn [db _]
    (articles/page-number db)))

(reg-sub
  ::pagination
  :<- [::sub-db]
  :<- [::current-page-number]
  (fn [[sub-db current-page-number] _]
    (when-let [p (::articles/pagination sub-db)]
      (pagination/generate-pagination current-page-number (/ (:total p) articles/page-size)))))
