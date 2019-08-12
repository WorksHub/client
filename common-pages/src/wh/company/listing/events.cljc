(ns wh.company.listing.events
  (:require
    #?(:cljs [wh.pages.core :refer [on-page-load] :as pages])
    [wh.graphql-cache :as cache :refer [reg-query]]
    [wh.company.listing.subs :as subs]
    [wh.company.listing.db :as companies])
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
                       :page_size   companies/page-limit
                       :search_term :$search_term
                       :sort        :$sort}
                      [[:pagination [:total :count]]
                       [:companies
                        [:id :slug :name :logo :size :descriptionHtml
                         :totalPublishedJobCount :totalPublishedIssueCount
                         [:tags [:id :label :slug :type :subtype :weight]]
                         [:locations [:city :country :countryCode :region :subRegion :state]]]]]]]})

(reg-query :companies fetch-companies-query)

(defn initial-query [db]
  (let [qps (:wh.db/query-params db)]
    ;; TODO search term from URL?
    [:companies {:page_number (subs/page-number qps)
                 :sort        (subs/company-sort qps)}]))

#?(:cljs
   (defmethod on-page-load :companies [db]
     (list (into [:graphql/query] (initial-query db)))))
