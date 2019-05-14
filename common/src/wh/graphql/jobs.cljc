(ns wh.graphql.jobs
  (#?(:clj :require :cljs :require-macros)
    [wh.graphql-macros :refer [defquery deffragment]]))

(def job-card-fields
  [:id :title :companyName :tagline
   [:location [:city :state :country :countryCode]]
   [:remuneration [:competitive :currency :timePeriod :min :max :equity]]
   :logo :tags :published :userScore
   :roleType :sponsorshipOffered :remote :companyId])

;; and the same for precompiled queries:

(deffragment jobCardFields :JobCard
  [:id :title :companyName :tagline
   [:location [:city :state :country :countryCode]]
   [:remuneration [:competitive :currency :timePeriod :min :max :equity]]
   :logo :tags :published :userScore
   :roleType :sponsorshipOffered :remote :companyId])

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

(defn show-public-only?
  [jobs]
  (every? nil? (map :company-name jobs)))
