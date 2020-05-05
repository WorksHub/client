(ns wh.company.jobs.subs
  (:require [re-frame.core :refer [reg-sub reg-sub-raw]]
            [wh.components.pagination :as pagination]
            [wh.company.listing.db :as listing]
            [wh.common.job :refer [format-job-location]]
            [wh.company.profile.db :as profile]
            [wh.re-frame.subs :refer [<sub]]
            [wh.company.jobs.db :as jobs])
  (#?(:clj :require :cljs :require-macros)
    [wh.re-frame.subs :refer [reaction]]))

(reg-sub
  ::company-slug
  (fn [db _]
    (jobs/company-slug db)))

(reg-sub
  ::page-number
  (fn [db _]
    (jobs/page-number db)))

(reg-sub-raw
  ::company
  (fn [_ _]
    (reaction
      (let [slug   (<sub [:wh/page-param :slug])
            result (<sub [:graphql/result :company-card {:slug slug}])]
        (profile/->company (:company result))))))

(reg-sub-raw
  ::jobs-raw
  (fn [db _]
    (reaction
      (let [slug        (<sub [::company-slug])
            page-number (<sub [::page-number])
            result      (<sub [:graphql/result :company-jobs-page {:slug slug :page_size jobs/page-size :page_number page-number}])]
        (:company result)))))

(reg-sub
  ::jobs
  :<- [::jobs-raw]
  (fn [jobs-raw _]
    (->> (get-in jobs-raw [:jobs :jobs])
         (map #(assoc % :display-location (format-job-location (:location %) (:remote %)))))))

(reg-sub
  ::total-number-of-jobs
  :<- [::jobs-raw]
  (fn [jobs-raw _]
    (or (get-in jobs-raw [:jobs :pagination :total]) 0)))

(reg-sub
  ::number-of-pages
  :<- [::total-number-of-jobs]
  (fn [total _]
    (pagination/number-of-pages jobs/page-size total)))

(reg-sub
  ::name
  :<- [::company]
  (fn [company _]
    (:name company)))

(reg-sub
  ::company-card
  :<- [::company]
  (fn [company _]
    (listing/->company company)))
