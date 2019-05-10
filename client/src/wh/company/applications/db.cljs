(ns wh.company.applications.db
  (:require
    [wh.db :as db]
    [wh.user.db :as user]))

(def apps-page-size 18)
(def jobs-page-size 300) ;; TODO desperately need pagination details returned from job query
(def latest-applied-jobs-page-size 10)

(defn get-applications-by-job
  [sub-db job-id]
  (get-in sub-db [::applications-by-job job-id :applications]))

(defn some-application-by-job
  [sub-db job-id p]
  (some #(when (p %) %) (get-applications-by-job sub-db job-id)))

(defn update-applications-by-job
  [sub-db job-id f]
  (update-in
   sub-db
   [::applications-by-job job-id]
   (fn [applications]
     (update applications :applications #(map f %)))))

(defn tab->states
  [tab admin?]
  (case tab
    :interviewing [:get_in_touch]
    :pending      (if admin? [:pending] [:pending :approved])
    :approved     [:approved]
    :pass         [:pass]
    :rejected     [:rejected]
    :hired        [:hired]
    nil))

(defn company-id [db]
  (if (user/company? db)
    (get-in db [::user/sub-db ::user/company-id])
    (get-in db [::db/page-params :id])))

(defn get-current-page
  [db]
  (cond (and (user/admin? db) (not (company-id db))) :homepage ;; :admin-applications
        (user/admin? db)                             :admin-company-applications
        (user/company? db)                           :company-applications ))

(defn company-view?
  [db]
  (and (company-id db)
       (not (get-in db [::db/query-params "job-id"]))))

(defn default-db
  [db]
  (merge (::sub-db db)
         {::current-page 1
          ::applications-by-job nil
          ::latest-applied-jobs nil
          ::has-permission? true
          ::current-tab (keyword (get-in db [::db/query-params "tab"] "pending"))
          ::applications nil}
         ;; when in company view, we never need to keep the jobs
         (when (company-view? db)
           {::jobs nil})))
