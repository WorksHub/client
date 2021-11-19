(ns wh.jobs.jobsboard.db)

(def default-db {::search #:wh.search {:available-role-types []
                                       :query                nil
                                       :role-types           #{}
                                       :tags                 #{}
                                       :cities               #{}
                                       :countries            #{}
                                       :regions              #{}
                                       :salary-type          nil
                                       :salary-range         nil
                                       :currency             nil
                                       :sponsorship          false
                                       :remote               false
                                       :only-mine            false
                                       :competitive          true
                                       :published            #{}}})

(def ssr-jobs-path [:wh.jobs.jobsboard.db/sub-db :wh.jobs.jobsboard.db/jobs])

(defn remove-ssr-jobs
  "we remove ssr-jobs from app-db so :wh.search/searching? sub works correctly"
  [app-db]
  (assoc-in app-db ssr-jobs-path nil))
