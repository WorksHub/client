(ns wh.login.github-callback.subs
  (:require
            [re-frame.core :refer [reg-sub]]
            [wh.login.github-callback.db :as github-callback]))

(reg-sub
  ::error
  (fn [db _]
    (get-in db [::github-callback/sub-db :error])))
