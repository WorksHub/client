(ns wh.login.subs
  (:require
    [clojure.spec.alpha :as s]
    [re-frame.core :refer [reg-sub]]
    [wh.common.specs.primitives :as primitives]
    [wh.db :as db]
    [wh.login.db :as login]))

(reg-sub
  ::magic-email
  (fn [db _]
    (get-in db [::login/sub-db ::login/magic-email])))

(reg-sub
  ::show-magic-form?
  (fn [db _]
    (= :email (get-in db [::db/page-params :step]))))

(reg-sub
  ::continue-to
  (fn [db _]
    (if (nil? (get-in db [::login/sub-db ::login/redirect]))
      "your dashboard"
      "the site")))

(reg-sub
  ::invalid-magic-email?
  :<- [::magic-email]
  (fn [e]
    (or
      (not e)
      (not (s/valid? ::primitives/email e)))))

(reg-sub
  ::magic-status
  (fn [db _]
    (get-in db [::login/sub-db ::login/magic-status])))

(reg-sub
  ::magic-success?
  (fn [db _]
    (get-in db [::db/query-params "sent"])))
