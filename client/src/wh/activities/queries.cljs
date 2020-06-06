(ns wh.activities.queries
  (:require-macros [wh.graphql-macros :refer [defquery]]))

(defquery all-activities-query
  {:venia/operation {:operation/type :query
                     :operation/name "all_activities"}
   :venia/queries   [[:query_activities
                      [[:activities [:id :verb [:actor [:id]] :to :date
                                     [:job [:id :title :slug :tagline :remote]]
                                     [:blog [:id :title :author :creator]]]]]]]})
