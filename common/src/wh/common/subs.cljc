(ns wh.common.subs
  (:require [re-frame.core :refer [reg-sub]]
            [wh.verticals :as verticals]))

;; This file should be where all 'core' subs are eventually migrated so that
;; they can be used in both client and server

;; USER

(reg-sub
  :user/sub-db
  (fn [db _]
    (get-in db [:wh.user.db/sub-db])))

(reg-sub
  :user/logged-in?
  (fn [db _]
    (boolean (get-in db [:wh.user.db/sub-db :wh.user.db/id]))))

(reg-sub
  :user/type
  (fn [db _]
    (get-in db [:wh.user.db/sub-db :wh.user.db/type])))

(reg-sub
  :user/admin?
  (fn [db _]
    (= (get-in db [:wh.user.db/sub-db :wh.user.db/type]) "admin")))

(reg-sub
  :user/company?
  (fn [db _]
    (= (get-in db [:wh.user.db/sub-db :wh.user.db/type]) "company")))

(reg-sub
  :user/company-id
  (fn [db _]
    (get-in db [:wh.user.db/sub-db :wh.user.db/company-id])))

(reg-sub
  :user/candidate?
  (fn [db _]
    (= (get-in db [:wh.user.db/sub-db :wh.user.db/type]) "candidate")))

(reg-sub
  :user/has-recommendations?
  :<- [:user/candidate?]
  (fn [candidate? _]
    candidate?))

(reg-sub
  :user/email
  (fn [db _]
    (get-in db [:wh.user.db/sub-db :wh.user.db/email])))

(reg-sub
 :user/id
 (fn [db _]
     (get-in db [:wh.user.db/sub-db :wh.user.db/id])))

(reg-sub
  :user/github-connected?
  (fn [db _]
    (-> (get-in db [:wh.user.db/sub-db :wh.user.db/github-id])
        boolean)))

(reg-sub
  :user/company-connected-github?
  (fn [db _]
    (get-in db [:wh.user.db/sub-db :wh.user.db/company :connected-github])))

(reg-sub
  :user/owner?
  (fn [db [_ id]]
    (and
      (= (get-in db [:wh.user.db/sub-db :wh.user.db/type]) "company")
      (= (get-in db [:wh.user.db/sub-db :wh.user.db/company-id]) id))))

(reg-sub
  :user/rejected?
  (fn [db _]
    (= "rejected"
       (get-in db [:wh.user.db/sub-db :wh.user.db/approval :status]))))

(reg-sub
  :user/name
  (fn [db _]
    (get-in db [:wh.user.db/sub-db :wh.user.db/name])))

(reg-sub
  :user/company
  (fn [db _]
    (get-in db [:wh.user.db/sub-db :wh.user.db/company])))

(reg-sub
  :user/company-onboarding-task-state
  (fn [db [_ id]]
    (some (fn [m] (when (= id (:id m)) (:state m)))
          (get-in db [:wh.user.db/sub-db :wh.user.db/company :onboarding-tasks]))))

(reg-sub
  :user/applied-jobs
  (fn [db _]
    (get-in db [:wh.user.db/sub-db :wh.user.db/applied-jobs])))

(reg-sub
  :user/liked-jobs
  (fn [db _]
    (get-in db [:wh.user.db/sub-db :wh.user.db/liked-jobs])))



;; MISC

(reg-sub
  :wh/platform-name
  (fn [db _]
    (:wh.db/platform-name db)))

(reg-sub
  :wh/vertical
  (fn [db _]
    (:wh.db/vertical db)))

(reg-sub
  :wh/facebook-app-id
  (fn [db _]
    (:wh.settings/facebook-app-id db)))

(reg-sub
  :wh/build
  (fn [db _]
    (:wh.db/build db)))

(reg-sub
  :wh/vertical-label
  :<- [:wh/vertical]
  (fn [vertical _]
    (verticals/config vertical :label-name)))

(reg-sub
  :wh/page
  (fn [db _]
    (:wh.db/page db)))

(reg-sub
  :wh/page-params
  (fn [db _]
    (:wh.db/page-params db)))

(reg-sub
  :wh/page-param
  :<- [:wh/page-params]
  (fn [params [_ param]]
    (get params param)))

(reg-sub
  :wh/query-params
  (fn [db _]
    (:wh.db/query-params db)))

(reg-sub
  :wh/query-param
  :<- [:wh/query-params]
  (fn [params [_ param]]
    (get params param)))

(reg-sub
  :wh/twitter
  :<- [:wh/vertical]
  (fn [vertical _]
    (verticals/config vertical :twitter)))

(reg-sub
  :wh/env
  (fn [db _]
    (:wh.settings/environment db)))
