(ns wh.graphql.tag
  (#?(:clj :require :cljs :require-macros)
    [wh.graphql-macros :refer [deffragment defquery def-query-template def-query-from-template]]))

(defquery create-tag-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "create_tag"}
   :venia/variables [{:variable/name "label" :variable/type :String!}
                     {:variable/name "type" :variable/type :tag_type!}]
   :venia/queries   [[:create_tag {:label :$label :type :$type}
                      [:label :id :slug :type]]]})
