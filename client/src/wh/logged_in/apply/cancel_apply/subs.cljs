(ns wh.logged-in.apply.cancel-apply.subs
  (:require [re-frame.core :refer [reg-sub]]
            [wh.logged-in.apply.cancel-apply.db :as cancel]))

(reg-sub ::sub-db (fn [db _] (::cancel/sub-db db)))

(reg-sub
  ::display-chatbot?
  :<- [::sub-db]
  (fn [db _]
    (::cancel/job db)))

(reg-sub
  ::step-taken?
  :<- [::sub-db]
  (fn [db [_ step]]
    (contains? (::cancel/steps-taken db) step)))

(reg-sub
  ::current-step
  :<- [::sub-db]
  (fn [db _]
    (::cancel/current-step db)))

(reg-sub
  ::updating?
  :<- [::sub-db]
  (fn [db _]
    (::cancel/updating? db)))

(reg-sub
  ::reason
  :<- [::sub-db]
  (fn [db _]
    (::cancel/reason db)))

(reg-sub
  ::reason-failed?
  :<- [::sub-db]
  (fn [db _]
    (::cancel/reason-failed? db)))

(reg-sub
  ::submit-success?
  :<- [::sub-db]
  (fn [db _]
    (::cancel/submit-success? db)))


