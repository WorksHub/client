(ns wh.graphql.jobs
  (:require [wh.graphql.fragments]
            [wh.graphql-cache :refer [reg-query]])
  (#?(:clj :require :cljs :require-macros)
    [wh.graphql-macros :refer [defquery def-query-template def-query-from-template]]))

(def job-card-fields
  ;; we'd like to use [:tags :fragment/tagFields] but it's not
  ;; defquery so it wouldn't compile properly
  ;; TODO: find proper solution for this problem
  [:id :slug :title :tagline [:tags [:id :slug :type :subtype :label :weight]]
   :published :userScore
   :roleType :sponsorshipOffered :remote :companyId
   [:company [:name :slug :logo]]
   [:location [:city :state :country :countryCode]]
   [:remuneration [:competitive :currency :timePeriod :min :max :equity]]])

(defquery recommended-jobs-for-job
  {:venia/operation {:operation/type :query
                     :operation/name "recommended_jobs_for_job"}
   :venia/variables [{:variable/name "job_id" :variable/type :ID!}]
   :venia/queries   [[:jobs {:filter_type "recommended"
                             :entity_type "job"
                             :entity_id   :$job_id
                             :page_size   4
                             :page_number 1}
                      :fragment/jobCardFields]]})

(def-query-template job-query
  {:venia/operation {:operation/type :query
                     :operation/name "job"}
   :venia/variables [{:variable/name "id"
                      :variable/type :ID}
                     {:variable/name "slug"
                      :variable/type :String}]
   :venia/queries   [[:job {:id   :$id
                            :slug :$slug}
                      $fields]]})

(def-query-from-template job-query--default job-query
                         {:fields :fragment/jobFields})

(reg-query :job job-query--default)

(def-query-from-template job-query--company job-query
                         {:fields [[:fragment/jobFields] :matchingUsers]})

(def-query-from-template job-query--candidate job-query
                         {:fields [[:fragment/jobFields] :userScore]})

(def-query-from-template job-query--company-managed-details job-query
                         {:fields [[:company [:id :managed :name]]]})

(defquery issues-query
  {:venia/operation {:operation/name "jobIssues"
                     :operation/type :query}
   :venia/variables [{:variable/name "id"
                      :variable/type :ID}]
   :venia/queries   [[:query_issues {:company_id :$id, :page_size 2}
                      [[:issues [:id :title :level [:repo [:primary_language]]]]]]]})
