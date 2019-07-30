(ns wh.graphql.issues
  (:require
    [wh.graphql.jobs])
  (#?(:clj :require :cljs :require-macros)
    [wh.graphql-macros :refer [deffragment defquery def-query-template def-query-from-template]]))

(deffragment issueFields :issue
  [:id :pr_count :url :number :body_html :title :viewer_contributed :published :status :level :created_at
   [:compensation [:amount :currency]]
   [:contributors [:id :name [:other_urls [:url]] [:github_info [:login]]]]
   [:repo [:owner :name :description :primary_language [:community [:readme_url :contributing_url]]]]
   [:company [:id :name :logo]]
   [:author [:login :name]]
   [:labels [:name]]])

(deffragment issueListFields :issue
  [:id :url :number :body :title :pr_count :level :status :published :created_at
   [:compensation [:amount :currency]]
   [:contributors [:id]]
   [:labels [:name]]
   [:company [:id :name :logo]]
   [:repo [:name :owner :primary_language]]])

(defquery fetch-issue
  {:venia/operation {:operation/type :query
                     :operation/name "issue"}
   :venia/variables [{:variable/name "id"
                      :variable/type :ID!}]
   :venia/queries [[:issue {:id :$id}
                    :fragment/issueFields]]})

(defquery fetch-issue-and-login
  {:venia/operation {:operation/type :query
                     :operation/name "issue"}
   :venia/variables [{:variable/name "id"
                      :variable/type :ID!}]
   :venia/queries [[:issue {:id :$id}
                    :fragment/issueFields]
                   [:me [[:githubInfo [:login]]]]]})

(defquery start-work-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "startWork"}
   :venia/variables [{:variable/name "id"
                      :variable/type :ID!}]
   :venia/queries   [[:start_work {:id :$id}
                      [:ok]]]})

(defquery update-issue-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "issue"}
   :venia/variables [{:variable/name "id"
                      :variable/type :ID!}
                     {:variable/name "level"
                      :variable/type :level}
                     {:variable/name "status"
                      :variable/type :issue_status}
                     {:variable/name "compensation"
                      :variable/type :compensation_input}]
   :venia/queries   [[:issue {:id           :$id
                              :level        :$level
                              :compensation :$compensation
                              :status       :$status}
                      [:id :level :status [:compensation [:amount :currency]]]]]})

(defquery update-issues-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "update_issues"}
   :venia/variables [{:variable/name "issues"
                      :variable/type {:type/kind       :list
                                      :type/required?  true
                                      :type.list/items {:type/type :update_issue_args_input!}}}]
   :venia/queries   [[:update_issues {:issues :$issues}
                      [[:issues [:fragment/issueListFields]]]]]})

(defquery fetch-company-issues
  {:venia/operation {:operation/type :query
                     :operation/name "issues"}
   :venia/variables [{:variable/name "id"
                      :variable/type :ID}
                     {:variable/name "page_number"
                      :variable/type :Int}
                     {:variable/name "page_size"
                      :variable/type :Int}
                     {:variable/name "published"
                      :variable/type :Boolean}]
   :venia/queries   [[:query_issues
                      {:company_id :$id
                       :published :$published
                       :page_number :$page_number
                       :page_size :$page_size}
                      [[:issues [:fragment/issueListFields]]
                       [:pagination [:total :count :page_size :page_number]]]]]})

(defquery fetch-company-issues--not-logged-in
  {:venia/operation {:operation/type :query
                     :operation/name "issues"}
   :venia/variables [{:variable/name "id"
                      :variable/type :ID!}
                     {:variable/name "page_number"
                      :variable/type :Int}
                     {:variable/name "page_size"
                      :variable/type :Int}
                     {:variable/name "published"
                      :variable/type :Boolean}]
   :venia/queries   [[:company {:id :$id}
                      [:name :logo]]
                     [:query_issues
                      {:company_id :$id
                       :published :$published
                       :page_number :$page_number
                       :page_size :$page_size}
                      [[:issues [:fragment/issueListFields]]
                       [:pagination [:total :count :page_size :page_number]]]]]})

(defquery fetch-company-issues--logged-in
  {:venia/operation {:operation/type :query
                     :operation/name "issues"}
   :venia/variables [{:variable/name "id"
                      :variable/type :ID!}
                     {:variable/name "page_number"
                      :variable/type :Int}
                     {:variable/name "page_size"
                      :variable/type :Int}
                     {:variable/name "published"
                      :variable/type :Boolean}]
   :venia/queries   [[:company {:id :$id}
                      [:name :logo]]
                     [:query_issues
                      {:company_id :$id
                       :published :$published
                       :page_number :$page_number
                       :page_size :$page_size}
                      [[:issues [:fragment/issueListFields]]
                       [:pagination [:total :count :page_size :page_number]]]]
                     [:me [:welcomeMsgs [:company [:connectedGithub]]]]]})

(defquery fetch-company-jobs
  {:venia/operation {:operation/type :query
                     :operation/name "companyJobs"}
   :venia/variables [{:variable/name "id"
                      :variable/type :String}]
   :venia/queries   [[:jobs {:filter_type "all"
                             :company_id :$id
                             :page_size 2
                             :page_number 1}
                      :fragment/jobCardFields]]})

(defquery fetch-company-jobs--lite
  {:venia/operation {:operation/type :query
                     :operation/name "companyJobs"}
   :venia/variables [{:variable/name "id"
                      :variable/type :String}]
   :venia/queries   [[:jobs {:filter_type "all"
                             :company_id :$id
                             :page_size 2
                             :page_number 1}
                      [:id :title :remote :slug
                       [:location [:city :state :country :countryCode]]]]]})
