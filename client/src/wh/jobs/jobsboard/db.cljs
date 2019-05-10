(ns wh.jobs.jobsboard.db)

(def default-db {::search #:wh.search{:available-role-types []
                                      :query nil
                                      :tag-part nil
                                      :matching-tags nil
                                      :tags-collapsed? true
                                      :role-types #{}
                                      :tags #{}
                                      :cities #{}
                                      :countries #{}
                                      :wh-regions #{} ; :us, :europe, or :rest-of-world
                                                      ; thusly called to distinguish it from location regions
                                      :salary-type nil
                                      :salary-range nil
                                      :currency "GBP"
                                      :sponsorship false
                                      :remote false
                                      :only-mine false
                                      :published #{}}})
