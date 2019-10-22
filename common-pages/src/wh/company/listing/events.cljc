(ns wh.company.listing.events
  (:require
    #?(:cljs [wh.pages.core :refer [on-page-load] :as pages])
    [wh.company.listing.db :as listing]
    [wh.company.listing.subs :as subs]
    [wh.components.pagination :as pagination]
    [wh.graphql-cache :as cache :refer [reg-query]]
    [wh.graphql.company] ;; included for fragments
    [wh.re-frame.events :refer [reg-event-fx]])
  (#?(:clj :require :cljs :require-macros)
    [wh.graphql-macros :refer [defquery]]))

(defquery fetch-companies-query
  {:venia/operation {:operation/type :query
                     :operation/name "companies"}
   :venia/variables [{:variable/name "page_number" :variable/type :Int}
                     {:variable/name "page_size" :variable/type :Int}
                     {:variable/name "search_term" :variable/type :String}
                     {:variable/name "sort" :variable/type :companies_sort}]
   :venia/queries   [[:companies
                      {:page_number :$page_number
                       :page_size   :$page_size
                       :search_term :$search_term
                       :sort        :$sort}
                      [[:pagination [:total :count]]
                       [:companies :fragment/companyCardFields]]]]})

(reg-query :companies fetch-companies-query)

(defn initial-query [db]
  (let [qps (:wh.db/query-params db)]
    ;; TODO search term from URL?
    [:companies {:page_number (pagination/qps->page-number qps)
                 :page_size   listing/page-size
                 :sort        (listing/company-sort qps)}]))

#?(:cljs
   (defmethod on-page-load :companies [db]
     (list (into [:graphql/query] (initial-query db)))))
