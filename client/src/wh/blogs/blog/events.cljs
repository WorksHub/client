(ns wh.blogs.blog.events
  (:require
    [re-frame.core :refer [dispatch path reg-event-db reg-event-fx]]
    [wh.blogs.blog.db :as blog]
    [wh.common.job]
    [wh.db :as db]
    [wh.graphql :refer [reg-query] :as graphql]
    [wh.graphql.jobs]
    [wh.pages.core :refer [on-page-load]]
    [wh.routes :as routes])
  (:require-macros
    [wh.graphql-macros :refer [defquery]]))

(def blog-interceptors (into db/default-interceptors
                             [(path ::blog/sub-db)]))

(defquery blog-query
  {:venia/operation {:operation/type :query
                      :operation/name "blog"}
   :venia/variables [{:variable/name "id"
                      :variable/type :ID!}]
   :venia/queries [[:blog {:id :$id}
                    [:id :title :feature :creator :published
                     :tags :htmlBody :readingTime :author
                     :originalSource :formattedCreationDate :upvoteCount
                     [:authorInfo [:name :summary :imageUrl
                                   [:otherUrls [:url :title]]]]]]]})

(reg-query :blog blog-query)

(defquery recommended-jobs-query
  {:venia/operation {:operation/type :query
                      :operation/name "recommended_jobs"}
   :venia/variables [{:variable/name "id"
                      :variable/type :ID}]
   :venia/queries [[:jobs {:filter_type "recommended"
                           :entity_id   :$id
                           :entity_type "blog"
                           :page_size   3
                           :page_number 1}
                    :fragment/jobCardFields]]})

(reg-query :recommended-jobs-for-blog recommended-jobs-query)

(defquery upvote-blog-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "upvote_blog"}
   :venia/variables [{:variable/name "id"
                      :variable/type :String!}]
   :venia/queries   [[:upvote_blog {:id :$id}
                      [:upvoted]]]})

(reg-event-db
  ::initialize-db
  blog-interceptors
  (fn [db _]
    blog/default-db))

(reg-event-db
  ::show-share-links
  blog-interceptors
  (fn [db [value]]
    (assoc db ::blog/share-links-shown? value)))

(reg-event-db
  ::toggle-author-info
  blog-interceptors
  (fn [db _]
    (update db ::blog/author-info-visible? not)))

(reg-event-db
  ::init-upvotes
  db/default-interceptors
  (fn [db _]
    (let [id (get-in db [:wh.db/page-params :id])
          blog (:blog (graphql/result db :blog {:id id}))]
      (assoc-in db [::blog/sub-db ::blog/upvotes id] (:upvote-count blog)))))

(reg-event-fx
  ::upvote
  db/default-interceptors
  (fn [{db :db} _]
    (let [id (get-in db [:wh.db/page-params :id])
          blog (:blog (graphql/result db :blog {:id id}))]
      (if (db/logged-in? db)
        {:graphql {:query      upvote-blog-mutation
                   :variables  {:id id}
                   :on-success [::upvote-success]
                   :on-failure [::upvote-failure]}
         :analytics/track ["Blog Boosted" (select-keys blog [:id :reading-time :title :tags :author :formatted-creation-date])]}
        {:show-auth-popup {:context :upvote
                           :redirect [:blog :params {:id id}]}}))))

(reg-event-db
  ::upvote-success
  db/default-interceptors
  (fn [db [result]]
    (let [id (get-in db [:wh.db/page-params :id])]
      (if (get-in result [:data :upvote_blog :upvoted])
        (update-in db [::blog/sub-db ::blog/upvotes id] inc)
        db))))

(reg-event-fx
  ::upvote-failure
  blog-interceptors
  (fn [_ [result]]
    {:dispatch [:error/set-global "Oops, failed to boost blog!"]}))

(defmethod on-page-load :blog [db]
  (let [id (get-in db [::db/page-params :id])]
    [[:graphql/query :blog {:id id} {:on-success [::init-upvotes]}]
     [:graphql/query :recommended-jobs-for-blog {:id id}]]))
