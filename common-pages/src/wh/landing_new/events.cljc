(ns wh.landing-new.events
  (:require [wh.graphql-cache :refer [reg-query]]
            [wh.landing-new.tags :as tags]
            [wh.components.activities.queries :as activities-queries]
            #?(:cljs [wh.pages.core :refer [on-page-load]])
            [wh.graphql.fragments]
            [wh.common.user :as user-common]
            [wh.slug :as slug]
            [re-frame.core :refer [reg-event-fx]]
            [wh.re-frame.subs :refer [<sub]]
            [wh.db :as db])
  (#?(:clj :require :cljs :require-macros)
    [wh.graphql-macros :refer [defquery]]))

(reg-query :recent-activities activities-queries/recent-activities-query)

(defn- get-activities-tags [{:keys [wh.db/query-params] :as db}]
  (let [public-feed   (get query-params "public")
        skills        (map (fn [{:keys [name]}]
                             {:slug (slug/tag-label->slug name)
                              :type "tech"})
                           (get-in db [:wh.user.db/sub-db :wh.user.db/skills]))
        selected-tags (tags/param->tags (get query-params "tags"))]
    ;; Selected tags have precedence over skills so once user selected
    ;; tags we don't need to send skills anymore. If tags are not present
    ;; we need only vertical or skills to create proper feed
    {:activities_tags (if (and (not public-feed)
                               (not (seq selected-tags))
                               (seq skills))
                        skills selected-tags)}))

(defn get-paging [{:keys [wh.db/query-params]}]
  (let [older-than  (get query-params "older-than")
        newer-than  (get query-params "newer-than")]
    (cond
      older-than {:older_than older-than}
      newer-than {:newer_than newer-than}
      :else      {})))

(defn recent-activities [{:keys [wh.db/vertical] :as db}]
  [:recent-activities (merge
                        {:vertical vertical}
                        (get-activities-tags db)
                        (get-paging db))])

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
                       :remote
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
                                  :remote
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

(defn recommended-jobs [_db]
  [:recommended-jobs-for-user {:page_size   3
                               :page_number 1}])

(defquery user-stats-query
  {:venia/operation {:operation/type :query
                     :operation/name "user_stats"}
   :venia/variables [{:variable/name "id"
                      :variable/type :ID}]
   :venia/queries   [[:query_user_stats {:id :$id}
                      [[:blogs_counted [:in_progress :published]]
                       [:applications_counted
                        [:in_progress :interview_stage :submitted]]
                       [:issues_counted [:in_progress :completed]]]]]})
(reg-query :user-stats user-stats-query)
(defn user-stats [_db]
  [:user-stats {}])

(reg-event-fx
  ::set-page-title
  db/default-interceptors
  (fn [{db :db} _]
    {:page-title {:page-name "Feed"
                  :vertical  (:wh.db/vertical db)}}))

(defn queries
  ([db]
   (concat [recent-activities
            top-blogs
            top-companies
            top-tags
            top-users
            recent-issues]
           (if (user-common/candidate? db)
             [recommended-jobs
              user-stats]
             [recent-jobs]))))

#?(:cljs
   (defmethod on-page-load :feed [db]
     (into [[:wh.pages.core/unset-loader]]
           (map #(into [:graphql/query] (% db)) (queries db)))))

#?(:cljs
   (defmethod on-page-load :homepage-dashboard [db]
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
  (fn [{_db :db} [older-than]]
    {:scroll-to-top true
     :dispatch      [:wh.events/nav--set-query-params
                     {"older-than" older-than
                      "newer-than" nil}]}))

(reg-event-fx
  ::set-newer-than
  db/default-interceptors
  (fn [{_db :db} [newer-than]]
    {:scroll-to-top true
     :dispatch      [:wh.events/nav--set-query-params
                     {"older-than"  nil
                      "newer-than" newer-than}]}))

(reg-event-fx
  ::set-public-feed
  db/default-interceptors
  (fn [{_db :db} []]
    {:scroll-to-top true
     :dispatch      [:wh.events/nav--set-query-param "public" true]}))
