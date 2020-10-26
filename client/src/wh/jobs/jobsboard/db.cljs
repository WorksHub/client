(ns wh.jobs.jobsboard.db)

(def default-db {::search #:wh.search {:available-role-types []
                                       :query                nil
                                       :tag-part             nil
                                       :matching-tags        nil
                                       :tags-collapsed?      true
                                       :role-types           #{}
                                       :tags                 #{}
                                       :cities               #{}
                                       :countries            #{}
                                       ;; :wh-regions thusly called to distinguish it from location regions
                                       ;; :us, :europe, or :rest-of-world
                                       :wh-regions           #{}
                                       :salary-type          nil
                                       :salary-range         nil
                                       :currency             nil
                                       :sponsorship          false
                                       :remote               false
                                       :only-mine            false
                                       :competitive          true
                                       :published            #{}}})
