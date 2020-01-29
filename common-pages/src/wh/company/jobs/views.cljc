(ns wh.company.jobs.views
  (:require
    [clojure.string :as str]
    [wh.company.jobs.db :as jobs]
    [wh.company.jobs.subs :as subs]
    [wh.company.listing.views :refer [company-card]]
    [wh.components.job :refer [job-card]]
    [wh.components.pagination :as pagination]
    [wh.re-frame.subs :refer [<sub]]))

(defn page
  []
  (let [logged-in?   (<sub [:user/logged-in?])
        has-applied? (some? (<sub [:user/applied-jobs]))
        company-id   (<sub [:user/company-id])
        admin?       (<sub [:user/admin?])
        jobs         (<sub [::subs/jobs])
        current-page (<sub [::subs/page-number])]
    [:div.main.company-jobs
     [company-card (<sub [::subs/company-card]) {:view-jobs-link? false}]
     [:h1 (str "All Jobs from " (<sub [::subs/name]))]
     [:div.company-jobs__grid-list
      (for [job jobs]
        ^{:key (:id job)}
        [job-card job {:logged-in?        logged-in?
                       :user-has-applied? has-applied?
                       :user-is-company?  (not (nil? company-id))
                       :user-is-owner?    (or admin? (= company-id (:company-id job)))
                       :apply-source      "company-jobs-job"}])]
     (when (and (not-empty jobs) (> (<sub [::subs/total-number-of-jobs]) jobs/page-size))
       [pagination/pagination
        current-page
        (pagination/generate-pagination current-page (<sub [::subs/number-of-pages]))
        :company-jobs
        (<sub [:wh/query-params])
        {:slug (<sub [::subs/company-slug])}])]))
