(ns wh.admin.queries
  (#?(:clj :require :cljs :require-macros)
   [wh.graphql-macros :refer [defquery]]))

(defquery set-approval-status-mutation
          {:venia/operation {:operation/type :mutation
                             :operation/name "update_user_approval_status"}
           :venia/variables [{:variable/name "id"
                              :variable/type :ID!}
                             {:variable/name "status"
                              :variable/type :String!}]
           :venia/queries [[:update_user_approval_status {:id :$id :status :$status}]]})