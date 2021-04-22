(ns wh.login.core
  (:require [cljs.loader :as loader]
            [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
            [wh.db :as db]
            [wh.login.events :as login-events]
            [wh.login.github-callback.events]
            [wh.login.github-callback.views :as github-callback]
            [wh.login.stackoverflow-callback.events]
            [wh.login.stackoverflow-callback.views :as stackoverflow-callback]
            [wh.login.twitter-callback.events]
            [wh.login.twitter-callback.views :as twitter-callback]
            [wh.login.views :as login]))

(def page-mapping
  {:github-callback        github-callback/page
   :stackoverflow-callback stackoverflow-callback/page
   :twitter-callback       twitter-callback/page
   :login                  login/page})

(reg-event-db
  ::initialize-page-mapping
  (fn [db _]
    (update db ::db/page-mapping merge page-mapping)))

(dispatch-sync [::login-events/initialize-db])
(dispatch-sync [::initialize-page-mapping])

(loader/set-loaded! :login)
