(ns wh.company.profile.events
  (:require
    [wh.graphql-cache :refer [reg-query]]
    #?(:cljs [wh.pages.core :refer [on-page-load] :as pages]))
  (#?(:clj :require :cljs :require-macros)
    [wh.graphql-macros :refer [defquery]]))

(defquery fetch-company-query
  {:venia/operation {:operation/type :query
                     :operation/name "company"}
   :venia/variables [{:variable/name "id"
                      :variable/type :ID!}]
   :venia/queries [[:company {:id :$id}
                    [:id :name :logo]]]})

(reg-query :company fetch-company-query)

(defn initial-query [db]
  [:company {:id (get-in db [:wh.db/page-params :id])}])

#?(:cljs
   (defmethod on-page-load :company [db]
     [(into [:graphql/query] (initial-query db))]))
