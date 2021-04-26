(ns wh.blogs.liked.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [wh.blogs.learn.db :as learn]
    [wh.blogs.liked.events :as events]
    [wh.graphql-cache :as graphql]))

(reg-sub
  ::liked-blogs
  (fn [db _]
    (let [params     (learn/params db)
          data-path  events/blogs-path
          cache-data (graphql/result db events/query-name params)]
      {:blogs    (get-in cache-data data-path)
       :loading? (= cache-data :executing)})))
