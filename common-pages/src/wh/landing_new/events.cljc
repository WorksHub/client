(ns wh.landing-new.events
  (:require [wh.graphql-cache :refer [reg-query]]
            [wh.landing-new.tags :as tags]
            [wh.components.activities.queries :as activities-queries]
            #?(:cljs [wh.pages.core :refer [on-page-load]])
            [wh.graphql.fragments]
            [re-frame.core :refer [reg-event-fx]]
            [wh.db :as db])
  (#?(:clj :require :cljs :require-macros)
    [wh.graphql-macros :refer [defquery]]))

(reg-query :recent-activities activities-queries/recent-activities-query)

(defn recent-activities [{:keys [wh.db/vertical wh.db/query-params] :as _db}]
  (let [tags (tags/param->tags (get query-params "tags"))]
    [:recent-activities {:activities_tags tags
                         :vertical        vertical}]))

(defquery top-blogs-query
  {:venia/operation {:operation/type :query
                     :operation/name "top_blogs"}
   :venia/variables [{:variable/name "vertical"
                      :variable/type :vertical}
                     {:variable/name "limit"
                      :variable/type :Int}]
   :venia/queries   [[:top_blogs {:vertical :$vertical
                                  :limit    :$limit}
                      [[:results [:id
                                  :title
                                  [:tags :fragment/tagFields]
                                  :creation_date
                                  :reading_time
                                  :upvote_count
                                  [:author_info [:name :image_url]]]]]]]})

(reg-query :top-blogs top-blogs-query)

(defn top-blogs [db]
  [:top-blogs {:vertical (:wh.db/vertical db)
               :limit    5}])

(defquery recent-issues-query
  {:venia/operation {:operation/type :query
                     :operation/name "recent_issues"}
   :venia/variables [{:variable/name "vertical"
                      :variable/type :vertical}
                     {:variable/name "limit"
                      :variable/type :Int}]
   :venia/queries   [[:recent_issues {:vertical :$vertical
                                      :limit    :$limit}
                      [[:results [:id
                                  [:compensation [:amount]]
                                  :title
                                  :level
                                  [:repo [:primary_language]]
                                  [:company [:name :slug :logo]]]]]]]})
(reg-query :recent-issues recent-issues-query)
(defn recent-issues [db]
  [:recent-issues {:vertical (:wh.db/vertical db)
                   :limit    3}])

(defquery top-companies-query
  {:venia/operation {:operation/type :query
                     :operation/name "top_companies"}
   :venia/variables [{:variable/name "vertical"
                      :variable/type :vertical}
                     {:variable/name "limit"
                      :variable/type :Int}]
   :venia/queries   [[:top_companies {:vertical :$vertical
                                      :limit    :$limit}
                      [[:results [:id
                                  :name
                                  :slug
                                  :logo
                                  :total_published_issue_count
                                  :total_published_job_count
                                  [:tags :fragment/tagFields]
                                  [:locations [:city :country]]]]]]]})
(reg-query :top-companies top-companies-query)
(defn top-companies [db]
  [:top-companies {:vertical (:wh.db/vertical db)
                   :limit    3}])

(defquery top-users-query
  {:venia/operation {:operation/type :query
                     :operation/name "top_users"}
   :venia/variables [{:variable/name "vertical"
                      :variable/type :vertical}
                     {:variable/name "limit"
                      :variable/type :Int}]
   :venia/queries   [[:top_users {:vertical :$vertical
                                  :limit    :$limit}
                      [[:results [:id
                                  :name
                                  :created
                                  :image_url
                                  :blog_count
                                  :issue_count]]]]]})
(reg-query :top-users top-users-query)
(defn top-users [db]
  [:top-users {:vertical (:wh.db/vertical db)
               :limit    3}])

(defquery recent-jobs-query
  {:venia/operation {:operation/type :query
                     :operation/name "recent_jobs"}
   :venia/variables [{:variable/name "vertical"
                      :variable/type :vertical}
                     {:variable/name "limit"
                      :variable/type :Int}]
   :venia/queries   [[:recent_jobs {:vertical :$vertical
                                    :limit    :$limit}
                      [[:results [:id
                                  :title
                                  :slug
                                  [:company_info [:id
                                                  :name
                                                  :slug
                                                  :logo
                                                  :total_published_job_count]]]]]]]})
(reg-query :recent-jobs recent-jobs-query)
(defn recent-jobs [db]
  [:recent-jobs {:vertical (:wh.db/vertical db)
                 :limit    3}])

(defquery top-tags-query
  {:venia/operation {:operation/type :query
                     :operation/name "top_tags"}
   :venia/variables [{:variable/name "vertical"
                      :variable/type :vertical}
                     {:variable/name "limit"
                      :variable/type :Int}]
   :venia/queries   [[:top_tags {:vertical :$vertical
                                 :limit    :$limit}
                      [[:results :fragment/tagFields]]]]})
(reg-query :top-tags top-tags-query)
(defn top-tags [db]
  [:top-tags {:vertical (:wh.db/vertical db)
              :limit    15}])

(reg-event-fx
  ::set-page-title
  db/default-interceptors
  (fn [{db :db} _]
    {:page-title {:page-name "Feed"
                  :vertical  (:wh.db/vertical db)}}))

#?(:cljs
   (defmethod on-page-load :feed [db]
     [[:wh.pages.core/unset-loader]
      (into [:graphql/query] (recent-activities db))
      (into [:graphql/query] (top-blogs db))
      (into [:graphql/query] (top-companies db))
      (into [:graphql/query] (top-tags db))
      (into [:graphql/query] (top-users db))
      (into [:graphql/query] (recent-issues db))
      (into [:graphql/query] (recent-jobs db))]))

#?(:cljs
   ;; renamed by wh.pages.router
   (defmethod on-page-load :homepage-not-logged-in [db]
     [[:wh.pages.core/unset-loader]
      [::set-page-title]
      (into [:graphql/query] (recent-activities db))
      (into [:graphql/query] (top-blogs db))
      (into [:graphql/query] (top-companies db))
      (into [:graphql/query] (top-tags db))
      (into [:graphql/query] (top-users db))
      (into [:graphql/query] (recent-issues db))
      (into [:graphql/query] (recent-jobs db))]))
