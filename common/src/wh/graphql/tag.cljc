(ns wh.graphql.tag
  (#?(:clj :require :cljs :require-macros)
    [wh.graphql-macros :refer [deffragment defquery def-query-template def-query-from-template]]))

(defquery create-tag-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "create_tag"}
   :venia/variables [{:variable/name "label" :variable/type :String!}
                     {:variable/name "type" :variable/type :tag_type!}
                     {:variable/name "subtype" :variable/type :tag_subtype}]
   :venia/queries   [[:create_tag {:label :$label :type :$type :subtype :$subtype}
                      [:label :id :slug :type :subtype]]]})

(defquery update-tag-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "update_tag"}
   :venia/variables [{:variable/name "id"
                      :variable/type :ID!}
                     {:variable/name "slug"
                      :variable/type :String}
                     {:variable/name "label"
                      :variable/type :String}
                     {:variable/name "type"
                      :variable/type :tag_type}
                     {:variable/name "subtype"
                      :variable/type :tag_subtype}]
   :venia/queries   [[:update_tag {:id      :$id
                                   :slug    :$slug
                                   :label   :$label
                                   :type    :$type
                                   :subtype :$subtype}
                      [:id :slug :label :type :subtype]]]})
