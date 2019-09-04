(ns wh.graphql.jobs
  (#?(:clj :require :cljs :require-macros)
    [wh.graphql-macros :refer [defquery deffragment def-query-template def-query-from-template]]))

(def job-card-fields
  [:id :slug :title :tagline :tags :published :userScore
   :roleType :sponsorshipOffered :remote :companyId
   [:company [:name :slug :logo]]
   [:location [:city :state :country :countryCode]]
   [:remuneration [:competitive :currency :timePeriod :min :max :equity]]])

(deffragment jobCardFields :Job
  [:id :slug :title :tagline :tags :published :userScore
   :roleType :sponsorshipOffered :remote :companyId
   [:company [:name :slug :logo]]
   [:location [:city :state :country :countryCode]]
   [:remuneration [:competitive :currency :timePeriod :min :max :equity]]])

(defquery recommended-jobs-for-job
  {:venia/operation {:operation/type :query
                     :operation/name "recommended_jobs_for_job"}
   :venia/variables [{:variable/name "job_id" :variable/type :ID!}]
   :venia/queries [[:jobs {:filter_type "recommended"
                           :entity_type "job"
                           :entity_id :$job_id
                           :page_size 4
                           :page_number 1}
                    :fragment/jobCardFields]]})

(defn add-interactions
  [liked-jobs applied-jobs jobs]
  (mapv #(assoc %
                :liked (contains? liked-jobs (:id %))
                :applied (contains? applied-jobs (:id %)))
        jobs))

(deffragment jobFields :Job
  [:id :slug :title :companyId :tagline :descriptionHtml :tags :roleType :manager
   ;; [:company [[:issues [:id :title [:labels [:name]]]]]] ; commented out until company is leonaized
   [:company [:logo :name :descriptionHtml :profileEnabled :slug
              [:tags [:label :type :subtype :slug]]]]
   [:location [:street :city :country :countryCode :state :postCode :longitude :latitude]]
   [:remuneration [:competitive :currency :timePeriod :min :max :equity]]
   :locationDescription :remote :sponsorshipOffered :applied :published])

(def-query-template job-query
                    {:venia/operation {:operation/type :query
                                       :operation/name "job"}
                     :venia/variables [{:variable/name "id"
                                        :variable/type :ID}
                                       {:variable/name "slug"
                                        :variable/type :String}]
                     :venia/queries [[:job {:id :$id
                                            :slug :$slug}
                                      $fields]]})

(def-query-from-template job-query--default job-query
                         {:fields :fragment/jobFields})

(def-query-from-template job-query--company job-query
                         {:fields [[:fragment/jobFields] :matchingUsers]})

(def-query-from-template job-query--candidate job-query
                         {:fields [[:fragment/jobFields] :userScore]})

(defquery issues-query
  {:venia/operation {:operation/name "jobIssues"
                     :operation/type :query}
   :venia/variables [{:variable/name "id"
                      :variable/type :ID}]
   :venia/queries [[:query_issues {:company_id :$id, :page_size 2}
                    [[:issues [:id :title :level [:repo [:primary_language]]]]]]]})
