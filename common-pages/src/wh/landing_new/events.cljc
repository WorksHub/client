(ns wh.landing-new.events
  (:require
    [wh.graphql-cache :refer [reg-query]]
    [wh.graphql.fragments])
  (#?(:clj :require :cljs :require-macros)
   [wh.graphql-macros :refer [defquery]]))

(defquery top-blogs-query
  {:venia/operation {:operation/type :query
                     :operation/name "top_blogs"}
   :venia/queries   [[:top_blogs
                      [[:results [:id
                                  :title
                                  [:tags :fragment/tagFields]
                                  :creation_date
                                  :reading_time
                                  :upvote_count
                                  [:author_info [:name :image_url]]]]]]]})
(reg-query :top-blogs top-blogs-query)
(defn top-blogs [_]
  [:top-blogs nil])
