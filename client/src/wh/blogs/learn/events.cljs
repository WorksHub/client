(ns wh.blogs.learn.events
  (:require
    [bidi.bidi :as bidi]
    [re-frame.core :refer [dispatch path reg-event-db reg-event-fx]]
    [wh.blogs.learn.db :as learn]
    [wh.common.cases :as cases]
    [wh.db :as db]
    [wh.graphql :refer [reg-query]]
    [wh.pages.core :refer [on-page-load] :as pages])
  (:require-macros
    [wh.graphql-macros :refer [defquery]]))

(def learn-interceptors (into db/default-interceptors
                              [(path ::learn/sub-db)]))

(defquery blogs-query
  {:venia/operation {:operation/type :query
                     :operation/name "blogs"}
   :venia/variables [{:variable/name "page_number"
                      :variable/type :Int}
                     {:variable/name "tag"
                      :variable/type :String}
                     {:variable/name "vertical"
                      :variable/type :vertical}]
   :venia/queries [[:blogs {:tag :$tag :page_size 24 :page_number :$page_number :vertical :$vertical}
                    [:totalCount [:results [:id :title :feature :tags :author :formattedCreationDate :readingTime :creator :upvoteCount :published]]]]]})

(reg-query :blogs blogs-query)

(reg-event-fx
  ::load-blogs
  db/default-interceptors
  (fn [{db :db} _]
    {:scroll-to-top true
     :dispatch [:graphql/query :blogs (learn/params db)]}))

(defmethod on-page-load :learn [db]
  [[::pages/unset-loader]
   [::load-blogs]])

(defmethod on-page-load :learn-by-tag [db]
  [[::pages/unset-loader]
   [::load-blogs]])
