(ns wh.common.company)

(defn permissions [db]
  (get-in db [:wh.user.db/sub-db :wh.user.db/company :permissions]))

(defn can-edit-jobs-after-first-job-published? [db]
  (contains? (permissions db) :can_edit_jobs_after_first_job_published))
