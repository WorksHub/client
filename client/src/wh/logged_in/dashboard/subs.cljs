(ns wh.logged-in.dashboard.subs
  (:require
    [goog.i18n.DateTimeFormat :as DateTimeFormat]
    [re-frame.core :refer [reg-sub]]
    [wh.graphql.jobs :as jobs]
    [wh.logged-in.dashboard.db :as dashboard])
  (:import [goog.i18n DateTimeFormat]))

(reg-sub
  ::dashboard
  (fn [db _]
    (::dashboard/sub-db db)))

;; TODO: https://app.clubhouse.io/functionalworks/story/2395/remove-add-interactions-brain-damage
(reg-sub
  ::jobs
  :<- [::dashboard]
  :<- [:wh.user/liked-jobs]
  :<- [:wh.user/applied-jobs]
  (fn [[dashboard liked-jobs applied-jobs] _]
    (->> (::dashboard/jobs dashboard)
         (jobs/add-interactions liked-jobs applied-jobs))))

(reg-sub
  ::applied-jobs
  :<- [::dashboard]
  :<- [:wh.user/liked-jobs]
  :<- [:wh.user/applied-jobs]
  (fn [[dashboard liked-jobs applied-jobs] _]
    (->> (::dashboard/applied-jobs dashboard)
         (jobs/add-interactions liked-jobs applied-jobs))))

(reg-sub
  ::show-public-only?
  :<- [::jobs]
  :<- [::applied-jobs]
  (fn [[jobs applied] _]
    (jobs/show-public-only? (concat jobs applied))))

(reg-sub
  ::display-applied-jobs?
  :<- [::applied-jobs]
  (fn [applied-jobs _]
    (boolean (seq applied-jobs))))

(reg-sub
  ::blogs
  (fn [db _]
    (get-in db [::dashboard/sub-db ::dashboard/blogs])))

(defn current-date []
  (.format (DateTimeFormat. DateTimeFormat/Format.LONG_DATE) (js/Date.)))

;; XXX: This is not strictly correct. A real subscription would fetch
;; this from db, and we would have an event firing at midnight
;; updating the date. But I would say it's not critical, so for the sake
;; of simplicity...
(reg-sub
  ::date
  (fn [db _]
    (current-date)))

(reg-sub
  ::loading-recommended?
  (fn [db _]
    (get-in db [::dashboard/sub-db ::dashboard/loading-recommended?])))

(reg-sub
  ::loading-error
  (fn [db _]
    (get-in db [::dashboard/sub-db ::dashboard/error])))
