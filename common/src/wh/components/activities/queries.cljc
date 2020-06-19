(ns wh.components.activities.queries
  (:require [wh.graphql.fragments])
  (#?(:clj :require :cljs :require-macros)
    [wh.graphql-macros :refer [defquery]]))

(defquery all-activities-query
  {:venia/operation {:operation/type :query
                     :operation/name "all_activities"}
   :venia/queries   [[:query_activities
                      [[:activities
                        [:id :verb [:actor [:id]] :to :date
                         [:feed_job [:id :title :slug :tagline :remote
                                     :sponsorship_offered :role_type :first_published
                                     [:tags :fragment/tagFields]
                                     [:remuneration
                                      [:currency :time_period :equity
                                       :min :max :competitive]]
                                     [:location [:city :state :country]]
                                     [:job_company [:name :logo :slug
                                                    :total_published_job_count]]]]
                         [:feed_company [:id :slug :name :description :size
                                         :total_published_job_count
                                         [:locations [:city :country :country_code
                                                      :region :sub_region :state]]
                                         [:tags :fragment/tagFields]]]
                         [:feed_issue [:id :title :status :level :body :pr_count
                                       :contributors_count :status :created_at
                                       [:compensation [:amount :currency]]
                                       [:issue_company [:id :name :slug :logo
                                                        :total_published_job_count]]
                                       [:tags :fragment/tagFields]]]
                         [:feed_blog [:id :title :author :creator :feature
                                      :reading_time :creation_date :upvote_count
                                      [:author_info [:name :image_url]]
                                      [:tags :fragment/tagFields]]]]]]]]})
