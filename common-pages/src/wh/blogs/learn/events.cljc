(ns wh.blogs.learn.events
  (:require
    [bidi.bidi :as bidi]
    [re-frame.core :refer [dispatch path reg-event-db reg-event-fx]]
    [wh.blogs.learn.db :as learn]
    [wh.common.cases :as cases]
    [wh.db :as db]
    [wh.graphql-cache :refer [reg-query]]
    #?(:cljs [wh.pages.core :refer [on-page-load] :as pages]))
  (#?(:clj :require :cljs :require-macros)
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
   :venia/queries   [[:blogs {:tag :$tag :page_size 24 :page_number :$page_number :vertical :$vertical}
                      [[:pagination [:total]]
                       [:blogs [:id :title :feature :tags :author :formattedCreationDate :readingTime :creator :upvoteCount :published]]]]]})

(reg-query :blogs blogs-query)

(defn initial-query [db]
  [:blogs (learn/params db)])

(reg-event-fx
  ::load-blogs
  db/default-interceptors
  (fn [{db :db} _]
    {:scroll-to-top true
     :dispatch (into [:graphql/query] (initial-query db))}))

#?(:cljs
   (defmethod on-page-load :learn [db]
     [[:wh.pages.core/unset-loader]
      [::load-blogs]]))

#?(:cljs
   (defmethod on-page-load :learn-by-tag [db]
     [[:wh.pages.core/unset-loader]
      [::load-blogs]]))
