(ns wh.login.core
  (:require
    [cljs.loader :as loader]
    [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
    [wh.db :as db]
    [wh.login.events :as login-events]
    [wh.login.get-started.events]
    [wh.login.get-started.views :as get-started]
    [wh.login.github-callback.events]
    [wh.login.github-callback.views :as github-callback]
    [wh.login.invalid-magic-link.views :as invalid-magic-link]
    [wh.login.policy.views :as policy]
    [wh.login.views :as login]))

(def page-mapping
  {:github-callback github-callback/page
   :invalid-magic-link invalid-magic-link/page
   :login login/page
   :privacy-policy policy/page
   :get-started get-started/page})

(reg-event-db
  ::initialize-page-mapping
  (fn [db _]
    (update db ::db/page-mapping merge page-mapping)))

(dispatch-sync [::login-events/initialize-db])
(dispatch-sync [::initialize-page-mapping])

(loader/set-loaded! :login)
