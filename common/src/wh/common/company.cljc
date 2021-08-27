(ns wh.common.company
  (:require [wh.common.user :as user]
            [wh.verticals :as verticals]))

(defn permissions [db]
  (get-in db [:wh.user.db/sub-db :wh.user.db/company :permissions]))

(defn can-edit-jobs-after-first-job-published? [db]
  (or (user/admin? db)
      (contains? (permissions db) :can_edit_jobs_after_first_job_published)))

(defn profile-published? [company]
  (boolean (:has-published-profile company)))

(defn members-emails [company]
  (-> (get-in company [:integrations :email])
      not-empty))

(defn ->platform-name [company]
  (->> company
       :vertical
       name
       verticals/vertical-config
       :platform-name))

(defn manager-email [c] (:manager c))
(defn hubspot-id [c] (get-in c [:hubspot :id]))

(defn has-published-profile? [c] (:has-published-profile c))

;; greenhouse

(defn greenhouse-token [c] (get-in c [:integrations :greenhouse :token]))
(defn has-greenhouse? [c] (-> c greenhouse-token boolean))

;; workable

(defn workable-subdomain [c] (get-in c [:integrations :workable :account-subdomain]))
(defn has-workable? [c] (-> c workable-subdomain boolean))
(defn workable-token [c] (get-in c [:integrations :workable :token]))
