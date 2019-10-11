(ns wh.company.listing.subs
  (:require
    [re-frame.core :refer [reg-sub reg-sub-raw]]
    [wh.re-frame.subs :refer [<sub]]
    [wh.company.listing.db :as listing])
  (#?(:clj :require :cljs :require-macros)
    [wh.re-frame.subs :refer [reaction]]))

(reg-sub ::db (fn [db _] db))

(defn page-number
  [qps]
  (let [p (get qps "page" "1")]
    #?(:cljs (js/parseInt p)
       :clj (Integer. p))))

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
  :<- [:wh.subs/query-params]
  (fn [qps _]
    (page-number qps)))
