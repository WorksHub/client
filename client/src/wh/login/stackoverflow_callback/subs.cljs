(ns wh.login.stackoverflow-callback.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [wh.login.stackoverflow-callback.db :as stackoverflow-callback]))

(reg-sub
  ::error?
  (fn [db _]
    (get-in db [::stackoverflow-callback/sub-db :error?])))
