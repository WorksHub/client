(ns wh.graphql.company
  (:require
    [wh.graphql.fragments]
    [wh.graphql-cache :refer [reg-query]])
  (#?(:clj :require :cljs :require-macros)
    [wh.graphql-macros :refer [defquery def-query-template def-query-from-template]]))

(defquery fetch-company-query
  {:venia/operation {:operation/type :query
                     :operation/name "company"}
   :venia/variables [{:variable/name "slug"
                      :variable/type :String!}]
   :venia/queries [[:company {:slug :$slug}
                    [:id :slug :name :logo :profileEnabled :descriptionHtml :size
                     :foundedYear :howWeWork :additionalTechInfo :hasPublishedProfile
                     [:techScales [:testing :ops :timeToDeploy]]
                     [:locations [:city :country :countryCode :region :subRegion :state]]
                     [:tags :fragment/tagFields]
                     [:videos [:youtubeId :thumbnail :description]]
                     [:images [:url :width :height]]]]]})

(defquery fetch-company-query--card
  {:venia/operation {:operation/type :query
                     :operation/name "company"}
   :venia/variables [{:variable/name "slug"
                      :variable/type :String!}]
   :venia/queries [[:company {:slug :$slug}
                    :fragment/companyCardFields]]})

(defquery fetch-company-blogs-and-issues-query
  {:venia/operation {:operation/type :query
                     :operation/name "company"}
   :venia/variables [{:variable/name "slug"
                      :variable/type :String!}]
   :venia/queries [[:company {:slug :$slug}
                    [[:blogs {:pageSize 2 :pageNumber 1}
                      [[:blogs
                        [:id :title :feature :author :formattedCreationDate :readingTime
                         :upvoteCount :creator :published
                         [:tags :fragment/tagFields]]]
                       [:pagination [:total]]]]
                     [:jobs {:pageSize 2 :pageNumber 1}
                      [[:jobs
                        [:fragment/jobCardFields]]
                       [:pagination [:total]]]]
                     [:issues {:pageSize 2 :pageNumber 1 :published true}
                      [[:issues
                        [:id :url :number :body :title :pr_count :level :status :published :created_at
                         [:compensation [:amount :currency]]
                         [:contributors [:id]]
                         [:labels [:name]]
                         [:repo [:name :owner :primary_language]]]]
                       [:pagination [:total]]]]
                     [:repos {:pageSize 10 :pageNumber 1 :hasIssues false}
                      [[:repos
                        [:github_id :name :description :primary_language :owner]]]]]]]})

(defquery fetch-all-company-jobs-query
  {:venia/operation {:operation/type :query
                     :operation/name "company"}
   :venia/variables [{:variable/name "slug"
                      :variable/type :String!}
                     {:variable/name "total"
                      :variable/type :Int!}]
   :venia/queries [[:company {:slug :$slug}
                    [[:jobs {:pageSize :$total}
                      [[:jobs
                        [:fragment/jobCardFields]]
                       [:pagination [:total]]]]]]]})

(defquery fetch-company-jobs-page-query
  {:venia/operation {:operation/type :query
                     :operation/name "company"}
   :venia/variables [{:variable/name "slug"
                      :variable/type :String!}
                     {:variable/name "page_size"
                      :variable/type :Int!}
                     {:variable/name "page_number"
                      :variable/type :Int!}]
   :venia/queries [[:company {:slug :$slug}
                    [[:jobs {:pageSize :$page_size :pageNumber :$page_number}
                      [[:jobs
                        [:fragment/jobCardFields]]
                       [:pagination [:total]]]]]]]})

(defquery fetch-company-articles-page-query
  {:venia/operation {:operation/type :query
                     :operation/name "company"}
   :venia/variables [{:variable/name "slug"
                      :variable/type :String!}
                     {:variable/name "page_size"
                      :variable/type :Int!}
                     {:variable/name "page_number"
                      :variable/type :Int!}]
   :venia/queries [[:company {:slug :$slug}
                    [:totalPublishedJobCount
                     [:blogs {:pageSize :$page_size :pageNumber :$page_number}
                      [[:blogs
                        [:fragment/blogCardFields]]
                       [:pagination [:total]]]]]]]})

(defquery analytics-query
  {:venia/operation {:operation/name "jobAnalytics"
                     :operation/type :query}
   :venia/variables [{:variable/name "company_id"
                      :variable/type :ID}]
   :venia/queries [[:job_analytics {:company_id :$company_id
                                    :granularity 0
                                    :num_periods 0}
                    [:granularity
                     [:applications [:date :count]]
                     [:profileViews [:date :count]]]]]})

(reg-query :company fetch-company-query)
(reg-query :company-card fetch-company-query--card)
(reg-query :company-issues-and-blogs fetch-company-blogs-and-issues-query)
(reg-query :company-jobs-page fetch-company-jobs-page-query)
(reg-query :company-articles-page fetch-company-articles-page-query)
(reg-query :all-company-jobs fetch-all-company-jobs-query)
(reg-query :company-stats analytics-query)

(def create-company-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "create_company"}
   :venia/variables [{:variable/name "create_company"
                      :variable/type :CreateCompanyInput!}]
   :venia/queries   [[:create_company {:create_company :$create_company}
                      [:id]]]})

(defn update-company-mutation-with-fields
  [fields]
  {:venia/operation {:operation/type :mutation
                     :operation/name "update_company"}
   :venia/variables [{:variable/name "update_company"
                      :variable/type :UpdateCompanyInput!}]
   :venia/queries   [[:update_company {:update_company :$update_company}
                      fields]]})

(def update-company-mutation (update-company-mutation-with-fields [:id]))

(def add-new-company-user-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "create_company_user"}
   :venia/variables [{:variable/name "create_company_user"
                      :variable/type :CreateCompanyUserInput!}]
   :venia/queries   [[:create_company_user {:create_company_user :$create_company_user}
                      [:name :email :id]]]})

(def delete-user-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "delete_user"}
   :venia/variables [{:variable/name "id"
                      :variable/type :ID!}]
   :venia/queries [[:delete_user {:id :$id}]]})

(def delete-integration-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "delete_integration"}
   :venia/variables [{:variable/name "integration"
                      :variable/type :integration!}
                     {:variable/name "company_id"
                      :variable/type :ID!}]
   :venia/queries [[:delete_integration {:integration :$integration :company_id :$company_id}]]})

(def set-task-as-read-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "setTaskAsRead"}
   :venia/variables [{:variable/name "task"
                      :variable/type :company_onboarding_task_id!}]
   :venia/queries [[:setTaskAsRead {:task :$task}
                    [[:onboardingTasks [:id :state]]]]]})

(def default-company-fields
  [:id :slug :name :manager :descriptionHtml
   :logo :package :vertical :autoApprove :permissions
   :paidOfflineUntil :disabled :profileEnabled
   [:videos [:youtubeId :thumbnail :description]]
   [:payment [:billingPeriod :expires [:card [:last4Digits :brand [:expiry [:month :year]]]]]]
   [:users [:name :email :id]]
   [:integrations [:email [:greenhouse [:enabled]] [:slack [:enabled]] [:workable [:enabled]]]]])

(defquery edit-job-company-query
  {:venia/operation {:operation/name "company"
                     :operation/type :query}
   :venia/variables [{:variable/name "company_id"
                      :variable/type :ID}]
   :venia/queries   [[:company {:id :$company_id}
                      [:descriptionHtml :logo :slug
                       [:tags :fragment/tagFields]
                       [:integrations [[:greenhouse [:enabled [:jobs [:id :name]]]]
                                       [:workable [:enabled :accountSubdomain
                                                   [:jobs [:id :name]]
                                                   [:accounts [:subdomain :name]]]]]]]]]})

(defn company-query
  ([id fields]
   {:venia/queries [[:company {:id id} fields]]})
  ([id]
   (company-query id default-company-fields)))

(defn company-query-with-payment-details
  [id]
  (company-query id (concat default-company-fields
                            [:connectedGithub
                             [:payment [:billingPeriod :expires [:card [:last4Digits :brand [:expiry [:month :year]]]]
                                        [:coupon [:discountAmount :discountPercentage :description :duration]]]]
                             [:offer [:recurringFee :placementPercentage :acceptedAt]]
                             [:pendingOffer [:recurringFee :placementPercentage]]
                             [:invoices [:date :url :amount]]])))

(defn job-query
  [id fields]
  {:venia/queries [[:job {:id id} fields]]})

(def create-job-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "create_job"}
   :venia/variables [{:variable/name "create_job"
                      :variable/type :CreateJobInput!}]
   :venia/queries   [[:create_job {:create_job :$create_job}
                      [:id :slug]]]})

(def update-job-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "update_job"}
   :venia/variables [{:variable/name "update_job"
                      :variable/type :UpdateJobInput!}]
   :venia/queries   [[:update_job {:update_job :$update_job}
                      [:id :slug :published]]]})

(defn all-company-jobs-query-fragment
  ([company-id page-size page-number & [fields]]
   (all-company-jobs-query-fragment {:company-id  company-id
                                     :page-size   page-size
                                     :page-number page-number
                                     :fields      fields}))
  ([{:keys [company-id page-size page-number fields published manager vertical]}]
   [:jobs (merge {:filter_type "all"
                  :company_id  company-id
                  :page_size   page-size
                  :page_number page-number}
                 (when published
                   {:published published})
                 (when manager
                   {:manager manager})
                 (when vertical
                   {:vertical vertical}))
    (or fields [:id :slug :title :firstPublished [:tags [:id :slug :type :subtype :label :weight]] :published
                [:location [:city :state :country :countryCode]]
                [:stats [:applications :views :likes]]])]))

(defn all-company-jobs-query
  [args]
  {:venia/queries [(all-company-jobs-query-fragment args)]})

(defquery sync-issues-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "sync_issues"}
   :venia/variables [{:variable/name "name" :variable/type :String!}
                     {:variable/name "owner" :variable/type :String!}
                     {:variable/name "force" :variable/type :Boolean}
                     {:variable/name "publish_all" :variable/type :Boolean}]
   :venia/queries   [[:sync {:name :$name :owner :$owner :force :$force :publish_all :$publish_all}
                      [:id :time_started :total_issue_count :time_finished]]]})

(defquery sync-repos
  {:venia/operation {:operation/type :query
                     :operation/name "issues"}
   :venia/variables [{:variable/name "id"
                      :variable/type :ID}
                     {:variable/name "published"
                      :variable/type :Boolean}]
   :venia/queries   [[:company {:id :$id}
                      [:id [:integrations [[:slack [:enabled]]]]]]
                     [:github_repositories
                      [[:repositories
                        [:name :owner :owner_avatar :description :primary_language :stargazers :fork :open_issues_count
                         :has_unpublished_issues]]]]
                     [:query_issues
                      {:company_id :$id
                       :published  :$published}
                      [[:pagination [:total]]]]]})

(defquery publish-company-profile-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "publish_profile"}
   :venia/variables [{:variable/name "id" :variable/type :String!}
                     {:variable/name "profile_enabled" :variable/type :Boolean!}]
   :venia/queries [[:publish_profile {:id :$id :profile_enabled :$profile_enabled}
                    [:success :profile_enabled]]]})
