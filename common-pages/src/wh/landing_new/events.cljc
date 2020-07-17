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
  (let [tags       (tags/param->tags (get query-params "tags"))
        older-than (get query-params "older-than")
        newer-than (get query-params "newer-than")]

    [:recent-activities (merge
                          {:activities_tags tags
                           :vertical        vertical}
                          (cond
                            older-than {:older_than older-than}
                            newer-than {:newer_than newer-than}
                            :else      {}))]))

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

(defquery recommended-jobs-for-user
  {:venia/operation {:operation/type :query
                     :operation/name "recommended_jobs_for_user"}
   :venia/variables [{:variable/name "page_size" :variable/type :Int}
                     {:variable/name "page_number" :variable/type :Int}]
   :venia/queries   [[:jobs {:filter_type "recommended"
                             :entity_type "user"
                             :page_size   :$page_size
                             :page_number :$page_number}
                      [:id
                       :title
                       :slug
                       :userScore
                       [:company [:id
                                  :name
                                  :slug
                                  :logo
                                  :totalPublishedJobCount]]]]]})

(reg-query :recommended-jobs-for-user recommended-jobs-for-user)

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

(defn recommended-jobs [db]
  [:recommended-jobs-for-user {:page_size   3
                               :page_number 1}])

(reg-event-fx
  ::set-page-title
  db/default-interceptors
  (fn [{db :db} _]
    {:page-title {:page-name "Feed"
                  :vertical  (:wh.db/vertical db)}}))

(defn queries
  [db]
  (conj [recent-activities
         top-blogs
         top-companies
         top-tags
         top-users
         recent-issues]
        (if (= "candidate" (get-in db [:wh.user.db/sub-db :wh.user.db/type]))
          recommended-jobs
          recent-jobs)))

#?(:cljs
   (defmethod on-page-load :feed [db]
     (into [[:wh.pages.core/unset-loader]]
           (map #(into [:graphql/query] (% db)) (queries db)))))

#?(:cljs
   ;; renamed by wh.pages.router
   (defmethod on-page-load :homepage-not-logged-in [db]
     (into [[:wh.pages.core/unset-loader]
            [::set-page-title]]
           (map #(into [:graphql/query] (% db)) (queries db)))))

(reg-event-fx
  ::set-older-than
  db/default-interceptors
  (fn [{db :db} [older-than]]
    {:scroll-to-top true
     :dispatch      [:wh.events/nav--set-query-params
                     {"older-than"  older-than
                      "newer-than" nil}]}))

(reg-event-fx
  ::set-newer-than
  db/default-interceptors
  (fn [{db :db} [newer-than]]
    {:scroll-to-top true
     :dispatch      [:wh.events/nav--set-query-params
                     {"older-than"  nil
                      "newer-than" newer-than}]}))
