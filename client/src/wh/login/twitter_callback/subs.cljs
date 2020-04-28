(ns wh.login.twitter-callback.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [wh.login.twitter-callback.db :as twitter-callback]))

(reg-sub
  ::error?
  (fn [db _]
    (get-in db [::twitter-callback/sub-db :error?])))
