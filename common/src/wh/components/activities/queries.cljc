(ns wh.components.activities.queries
  (:require [wh.graphql.fragments])
  (#?(:clj :require :cljs :require-macros)
    [wh.graphql-macros :refer [defquery]]))

(defquery recent-activities-query
  {:venia/operation {:operation/type :query
                     :operation/name "recent_activities"}
   :venia/variables [{:variable/name "activities_tags"
                      :variable/type {:type/kind       :list
                                      :type.list/items {:type/type
                                                        :activity_tag_input}}}
                     {:variable/name "vertical"
                      :variable/type :vertical}
                     {:variable/name "older_than"
                      :variable/type :ID}
                     {:variable/name "newer_than"
                      :variable/type :ID}]
   :venia/queries   [[:query_activities {:activities_tags :$activities_tags
                                         :older_than      :$older_than
                                         :newer_than      :$newer_than
                                         :vertical        :$vertical}
                      [:first_page :last_page
                       [:activities
                        [:id :verb :to :date :description
                         [:actor [:id [:actor_data
                                       [:id :slug :name :actor_type :image_url
                                        :creation_date :total_published_job_count
                                        :total_published_issue_count]]]]
                         [:feed_job [:id :title :slug :tagline :remote
                                     :sponsorship_offered :role_type :first_published
                                     [:tags :fragment/tagFields]
                                     [:remuneration
                                      [:currency :time_period :equity
                                       :min :max :competitive]]
                                     [:location [:city :state :country]]
                                     [:job_company [:name :image_url :slug
                                                    :total_published_job_count]]]]
                         [:feed_company [:id :slug :name :description :size
                                         :total_published_job_count :logo :creation_date
                                         [:locations [:city :country :country_code
                                                      :region :sub_region :state]]
                                         [:tags :fragment/tagFields]]]
                         [:feed_issue [:id :title :status :level :body :pr_count
                                       :contributors_count :status :created_at
                                       [:repo [:primary_language]]
                                       [:compensation [:amount :currency]]
                                       [:issue_company [:id :name :slug :image_url
                                                        :total_published_issue_count]]
                                       [:issue_contributor [:id :name :image_url]]
                                       [:tags :fragment/tagFields]]]
                         [:feed_blog [:id :title :author :creator :feature
                                      :reading_time :creation_date :upvote_count
                                      [:author_info [:id :name :image_url]]
                                      [:tags :fragment/tagFields]]]]]]]]})
