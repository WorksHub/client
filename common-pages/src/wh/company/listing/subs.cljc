(ns wh.company.listing.subs
  (:require
    [re-frame.core :refer [reg-sub reg-sub-raw]]
    [wh.re-frame.subs :refer [<sub]]
    [wh.company.listing.db :as listing]
    [wh.components.pagination :as pagination]
    [wh.util :as util])
  (#?(:clj :require :cljs :require-macros)
    [wh.re-frame.subs :refer [reaction]]))

(reg-sub ::db (fn [db _] db))

(defn page-number
  [qps]
  (-> qps
      (get "page" "1")
      (util/parse-int)))

(reg-sub-raw
  ::companies
  (fn [_ _]
    (reaction
      (let [qps (<sub [:wh/query-params])
            {:keys [companies pagination] :as result}
            (:companies (<sub [:graphql/result :companies {:page_number (page-number qps)
                                                           :sort        (listing/company-sort qps)}]))]
        {:pagination pagination
         :companies (map listing/->company companies)}))))

(reg-sub
  ::current-page
  :<- [:wh/query-params]
  (fn [qps _]
    (page-number qps)))

(reg-sub
  ::total-number-of-results
  :<- [::companies]
  (fn [companies _]
    (-> companies :pagination :total)))

(reg-sub
  ::total-pages
  :<- [::total-number-of-results]
  (fn [total _]
    (int (#?(:cljs js/Math.ceil :clj Math/ceil) (/ total listing/page-limit)))))

(reg-sub
  ::pagination
  :<- [::current-page]
  :<- [::total-pages]
  (fn [[current-page total-pages] _]
    (pagination/generate-pagination current-page total-pages)))
