(ns wh.company.listing.subs
  (:require
    [re-frame.core :refer [reg-sub reg-sub-raw]]
    [wh.re-frame.subs :refer [<sub]]
    [wh.company.listing.db :as listing]
    [wh.components.pagination :as pagination]
    [wh.util :as util]
    [clojure.string :as str])
  (#?(:clj :require :cljs :require-macros)
    [wh.re-frame.subs :refer [reaction]]))

(reg-sub ::db (fn [db _] db))

(reg-sub-raw
  ::companies
  (fn [_ _]
    (reaction
      (let [qps (<sub [:wh/query-params])
            {:keys [companies pagination]}
            (:search-companies (<sub [:graphql/result :search_companies (listing/qps->query-body qps)]))]
        {:pagination pagination
         :companies (map listing/->company companies)}))))

(reg-sub
  ::current-page
  :<- [:wh/query-params]
  (fn [qps _]
    (pagination/qps->page-number qps)))

(reg-sub
  ::total-number-of-results
  :<- [::companies]
  (fn [companies _]
    (-> companies :pagination :total)))

(reg-sub
  ::total-pages
  :<- [::total-number-of-results]
  (fn [total _]
    (pagination/number-of-pages listing/page-size total)))

(reg-sub
  ::pagination
  :<- [::current-page]
  :<- [::total-pages]
  (fn [[current-page total-pages] _]
    (pagination/generate-pagination current-page total-pages)))

(reg-sub
  ::companies-count-str
  :<- [::total-number-of-results]
  :<- [::current-page]
  (fn [[total-count current-page-number] _]
    (or (pagination/results-label "companies" total-count current-page-number listing/page-size)
        "Loading...")))

(reg-sub
  ::loading?
  :<- [::companies]
  (fn [companies _]
    (nil? (:companies companies))))

(reg-sub
  ::sorting-by
  :<- [:wh/query-params]
  (fn [qps _]
    (keyword (listing/company-sort qps))))

(reg-sub
  ::live-jobs-only?
  :<- [:wh/query-params]
  (fn [qps _]
    (listing/live-jobs-only qps)))

(reg-sub
  ::sorting-options
  (fn [db _]
    [{:id :popular  :label  "Popular"}
     {:id :updated  :label  "Most Recent"}
     {:id :alpha    :label  "Name"}]))
