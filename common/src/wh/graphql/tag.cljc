(ns wh.graphql.tag
  (:require
    [wh.graphql-cache :as cache :refer [reg-query]])
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
                     {:variable/name "weight"
                      :variable/type :Float}
                     {:variable/name "type"
                      :variable/type :tag_type}
                     {:variable/name "subtype"
                      :variable/type :tag_subtype}]
   :venia/queries   [[:update_tag {:id      :$id
                                   :slug    :$slug
                                   :label   :$label
                                   :weight  :$weight
                                   :type    :$type
                                   :subtype :$subtype}
                      [:id :slug :label :type :subtype :weight]]]})

(defquery fetch-tags
  {:venia/operation {:operation/type :query
                     :operation/name "list_tags"}
   :venia/variables [{:variable/name "type"
                      :variable/type :tag_type}]
   :venia/queries [[:list_tags {:type :$type}
                    [[:tags :fragment/tagFields]]]]})

(reg-query :tags fetch-tags)

(defn tag-query [type-filter]
  (if type-filter
    [:tags {:type type-filter}]
    [:tags {}]))
