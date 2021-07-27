(ns wh.admin.articles.events
  (:require [re-frame.core :refer [path reg-event-db reg-event-fx]]
            [wh.admin.articles.db :as articles]
            [wh.common.cases :as cases]
            [wh.db :as db]
            [wh.graphql.fragments :as _fragments]
            [wh.pages.core :as pages :refer [on-page-load]])
  (:require-macros [wh.graphql-macros :refer [defquery]]))

(def articles-interceptors (into db/default-interceptors
                                 [(path ::articles/sub-db)]))

(defquery articles-query
  {:venia/operation {:operation/type :query
                     :operation/name "articles"}
   :venia/variables [{:variable/name "page_number" :variable/type :Int}
                     {:variable/name "page_size" :variable/type :Int}
                     {:variable/name "published" :variable/type :Boolean}]
   :venia/queries   [[:blogs
                      {:page_number :$page_number
                       :page_size   :$page_size
                       :published   :$published}
                      [[:pagination [:total :count]]
                       [:blogs [:id :title :verticals :published :author
                                :authorId :formattedDate :creatorId
                                [:authorInfo [:name :imageUrl]]
                                [:company [:slug :name]]
                                [:tags :fragment/tagFields]]]]]]})

(reg-event-fx
  ::fetch-articles
  db/default-interceptors
  (fn [{db :db} _]
    (if (get-in db [::articles/sub-db ::articles/in-progress?])
      {:db db}
      {:graphql {:query articles-query
                 :variables  {:page_number (articles/page-number db)
                              :page_size articles/page-size
                              :published false}
                 :on-success [::fetch-articles-success]
                 :on-failure [::fetch-articles-failure]}
       :db (assoc-in db [::articles/sub-db ::articles/in-progress?] true)})))

(defn transform-article
  [company]
  (cases/->kebab-case company))

(reg-event-fx
  ::fetch-articles-success
  articles-interceptors
  (fn [{db :db} [{{{blogs :blogs pagination :pagination} :blogs} :data}]]
    {:db (assoc db
                ::articles/articles (map transform-article blogs)
                ::articles/in-progress? false
                ::articles/pagination pagination)
     :scroll-to-top true}))

(reg-event-db
  ::fetch-articles-failure
  articles-interceptors
  (fn [db [_]]
    (assoc db
           ::articles/articles []
           ::articles/in-progress? false
           ::articles/error? true)))

(reg-event-fx
  ::initialize-db
  db/default-interceptors
  (fn [{db :db} _]
    {:db (update db ::articles/sub-db articles/initialize-db)}))

(defmethod on-page-load :admin-articles
  [db]
  [[::fetch-articles]])
