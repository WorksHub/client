(ns wh.register.subs
  (:require [re-frame.core :refer [reg-sub]]
            [wh.register.db :as register]))

(reg-sub
  ::sub-db
  (fn [db _]
    (register/db->sub-db db)))

(reg-sub
  ::name
  :<- [::sub-db]
  (fn [db _]
    (register/->name db)))

(reg-sub
  ::email
  :<- [::sub-db]
  (fn [db _]
    (register/->email db)))

(reg-sub
  ::stackoverflow-signup?
  (fn [db _]
    (register/stackoverflow-signup? db)))

(reg-sub
  ::submitting?
  :<- [::sub-db]
  (fn [db _]
    (register/->submitting db)))

(reg-sub
  ::error
  :<- [::sub-db]
  (fn [db _]
    (register/->error db)))

(reg-sub
  ::error-email
  :<- [::error]
  :<- [::email]
  :<- [::stackoverflow-signup?]
  (fn [[error email stackoverflow-signup?] _]
    (cond
      (and stackoverflow-signup? (empty? email)) "Provide your email"
      (= :duplicate-user error) "User with this email already exists"
      (= :invalid-arguments error) "Provide correct email"
      :else nil)))

(reg-sub
  ::error-name
  :<- [::error]
  (fn [error _]
    (case error
      :invalid-user-name "Provide correct name"
      nil)))

(reg-sub
  ::error-unhandled
  :<- [::error]
  :<- [::error-email]
  :<- [::error-name]
  (fn [[error error-email error-name] _]
    (if (and (not error-email) (not error-name) error)
      "An error has occurred, please try again later")))
