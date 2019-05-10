(ns wh.issue.edit.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [wh.issue.edit.db :as issue-edit]))

(reg-sub
  ::sub-db
  (fn [db _] (::issue-edit/sub-db db)))

(reg-sub
  ::updating?
  :<- [::sub-db]
  (fn [db _]
    (::issue-edit/updating? db)))

(reg-sub
  ::pending-level
  :<- [::sub-db]
  :<- [::current-issue]
  (fn [[db current-issue] _]
    (or (::issue-edit/pending-level db)
        (:level current-issue))))

(reg-sub
  ::pending-status
  :<- [::sub-db]
  :<- [::current-issue]
  (fn [[db current-issue] _]
    (or (::issue-edit/pending-status db)
        (:status current-issue))))

(reg-sub
  ::pending-compensation
  :<- [::sub-db]
  :<- [::current-issue]
  (fn [[db current-issue] _]
    (or (::issue-edit/pending-compensation db)
        (get-in current-issue [:compensation :amount])
        0)))

(reg-sub
  :edit-issue/displayed-dialog
  :<- [::sub-db]
  (fn [db _]
    (::issue-edit/displayed-dialog db)))

(reg-sub
  ::current-issue
  :<- [::sub-db]
  (fn [db _]
    (::issue-edit/current-issue db)))
