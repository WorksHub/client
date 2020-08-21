(ns wh.login.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [wh.login.db :as login]))

(reg-sub ::email (fn [db] (login/email db)))
(reg-sub ::submitting? (fn [db] (login/submitting? db)))
(reg-sub ::error (fn [db] (login/error db)))
(reg-sub ::email-sent? (fn [db] (login/email-sent? db)))
(reg-sub ::error-message :<- [::error] (fn [status _] (login/status->error status)))