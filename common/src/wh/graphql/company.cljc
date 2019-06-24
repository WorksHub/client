(ns wh.graphql.company
  (#?(:clj :require :cljs :require-macros)
   [wh.graphql-macros :refer [deffragment defquery def-query-template def-query-from-template]]))


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

(def default-company-fields
  [:id :name :manager :descriptionHtml
   :logo :package :vertical :autoApprove
   :paidOfflineUntil :disabled :profileEnabled
   [:videos [:youtubeId :thumbnail :description]]
   [:payment [:billingPeriod :expires [:card [:last4Digits :brand [:expiry [:month :year]]]]]]
   [:users [:name :email :id]]
   [:integrations [:email [:greenhouse [:enabled]] [:slack [:enabled]]]]])

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
                                        [:coupon [:discountAmount :discountPercentage]]]]
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

(defn fetch-tags [success]
  {:query      {:venia/operation {:operation/type :query
                                  :operation/name "jobs_search"}
                :venia/variables [{:variable/name "vertical" :variable/type :vertical}
                                  {:variable/name "search_term" :variable/type :String!}
                                  {:variable/name "page" :variable/type :Int!}]
                :venia/queries   [[:jobs_search {:vertical    :$vertical
                                                 :search_term :$search_term
                                                 :page        :$page}
                                   [[:facets [:attr :value :count]]]]]}
   :variables  {:search_term ""
                :page        1
                :vertical    "functional"}
   :on-success [success]})

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
    (or fields [:id :slug :title :firstPublished :tags :published
                [:location [:city :state :country :countryCode]]
                [:stats [:applications :views :likes]]])]))

(defn all-company-jobs-query
  [args]
  {:venia/queries [(all-company-jobs-query-fragment args)]})

(defquery sync-issues-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "fetch_issues_for_repository"}
   :venia/variables [{:variable/name "name" :variable/type :String!}
                     {:variable/name "owner" :variable/type :String!}]
   :venia/queries   [[:fetchGithubIssues {:name :$name :owner :$owner}]]})

(defquery sync-orgs-and-repos
  {:venia/queries
   [[:github_organisations {:dummy true}
     [[:organisations
       [:name :avatar_url
        [:repositories
         [:name :owner :description :primary_language :stargazers]]]]]]]})

(defquery publish-company-profile-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "publish_profile"}
   :venia/variables [{:variable/name "id" :variable/type :String!}
                     {:variable/name "profile_enabled" :variable/type :Boolean!}]
   :venia/queries [[:publish_profile {:id :$id :profile_enabled :$profile_enabled }
                    [:success :profile_enabled]]]})
