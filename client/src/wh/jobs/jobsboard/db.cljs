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
