(ns wh.blogs.liked.events
  (:require
    [re-frame.core :refer [dispatch path reg-event-db reg-event-fx]]
    [wh.blogs.learn.db :as learn]
    [wh.db :as db]
    [wh.graphql-cache :refer [reg-query]]
    #?(:cljs [wh.pages.core :refer [on-page-load] :as pages]))
  (#?(:clj :require :cljs :require-macros)
   [wh.graphql-macros :refer [defquery]]))

(defquery liked-blogs-query
  {:venia/operation {:operation/type :query
                     :operation/name "liked_blogs"}
   :venia/variables [{:variable/name "page_number"
                      :variable/type :Int}
                     {:variable/name "page_size"
                      :variable/type :Int}]
   :venia/queries   [[:blogs {:page_size   :$page_size
                              :page_number :$page_number
                              :filter_type "liked"}
                      [[:pagination [:total]]
                       [:blogs :fragment/blogCardFields]]]]})

(def query-name :liked-blogs)

(def blogs-path [:blogs :blogs])

(reg-query query-name liked-blogs-query)

(defn initial-query [db]
  [query-name (learn/params db) {:force true}])

(reg-event-fx
  ::load-liked-blogs
  db/default-interceptors
  (fn [{db :db} _]
    {:scroll-to-top true
     :dispatch      (into [:graphql/query] (initial-query db))}))

#?(:cljs
   (defmethod on-page-load :liked-blogs [db]
     [[:wh.pages.core/unset-loader]
      [::load-liked-blogs]]))