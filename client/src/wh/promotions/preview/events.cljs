(ns wh.promotions.preview.events
  (:require [wh.blogs.blog.events]
            [wh.graphql-cache :refer [reg-query]]
            [wh.graphql.company]
            [wh.graphql.issues]
            [wh.graphql.jobs]
            [wh.pages.core :refer [on-page-load]])
  (:require-macros [wh.graphql-macros :refer [defquery]]))


(defquery recent-promotions-query
  {:venia/operation {:operation/type :query
                     :operation/name "recent_promotions"}
   :venia/queries   [[:query_promotions [[:promotions
                                          [:id
                                           :object_type
                                           :object_id
                                           :channel
                                           :start_date
                                           :description]]]]]})

(reg-query :recent-promotions recent-promotions-query)

(defn recent-promotions [_]
  [:recent-promotions {}])

(defmethod on-page-load :promotions-preview [db]
  [(into [:graphql/query] (recent-promotions db))])
