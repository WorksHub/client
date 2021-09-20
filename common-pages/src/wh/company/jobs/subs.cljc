(ns wh.company.jobs.subs
  (:require [re-frame.core :refer [reg-sub reg-sub-raw]]
            [wh.common.job :as job]
            [wh.company.jobs.db :as jobs]
            [wh.company.listing.db :as listing]
            [wh.company.profile.db :as profile]
            [wh.components.pagination :as pagination]
            [wh.re-frame.subs :refer [<sub]])
  (#?(:clj :require :cljs :require-macros)
   [wh.re-frame.subs :refer [reaction]]))

(reg-sub
  ::sub-db
  (fn [db _]
    (::jobs/sub-db db)))

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

(reg-sub
  ::show-unpublished?
  :<- [:user/admin?]
  :<- [:user/company?]
  :<- [::sub-db]
  (fn [[admin? company? sub-db] _]
    (jobs/show-unpublished? admin? company? (::jobs/unpublished? sub-db))))

(reg-sub-raw
  ::jobs-raw
  (fn [_ _]
    (reaction
      (let [slug              (<sub [::company-slug])
            page-number       (<sub [::page-number])
            show-unpublished? (<sub [::show-unpublished?])
            published         (jobs/published show-unpublished?)
            result            (<sub [:graphql/result :company-jobs-page {:slug        slug
                                                                         :page_size   jobs/page-size
                                                                         :page_number page-number
                                                                         :published   published
                                                                         :sort        jobs/default-sort}])]
        (:company result)))))

(reg-sub
  ::jobs
  :<- [::jobs-raw]
  :<- [:user/liked-jobs]
  :<- [:user/applied-jobs]
  (fn [[jobs-raw liked-jobs applied-jobs] _]
    (some->> (get-in jobs-raw [:jobs :jobs])
             (map #(assoc % :display-location
                          (job/format-job-location (:location %) (:remote %))))
             (job/add-interactions liked-jobs applied-jobs))))

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
