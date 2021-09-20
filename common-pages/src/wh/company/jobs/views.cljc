(ns wh.company.jobs.views
  (:require #?(:cljs [wh.components.forms.views :refer [labelled-checkbox]])
            [wh.company.jobs.db :as jobs]
            [wh.company.jobs.events :as events]
            [wh.company.jobs.subs :as subs]
            [wh.company.listing.views :refer [company-card]]
            [wh.components.job :refer [job-card]]
            [wh.components.pagination :as pagination]
            [wh.re-frame.subs :refer [<sub]]))

(defn page []
  (let [logged-in?   (<sub [:user/logged-in?])
        has-applied? (some? (<sub [:user/applied-jobs]))
        admin?       (<sub [:user/admin?])
        company?     (<sub [:user/company?])
        company-id   (<sub [:user/company-id])
        jobs         (<sub [::subs/jobs])
        current-page (<sub [::subs/page-number])]
    [:div.main.company-jobs
     [company-card (<sub [::subs/company-card]) {:view-jobs-link? false}]
     [:section
      [:h1 (str "All Jobs from " (<sub [::subs/name]))]
      #?(:cljs
         (when (or admin? company?)
           [:div.control
            [labelled-checkbox
             (<sub [::subs/show-unpublished?])
             {:label     "Show Unpublished Jobs"
              :on-change [::events/toggle-unpublished]}]]))]
     [:section
      [:div.card-grid-list
       (cond
         (seq jobs)
         (for [job jobs]
           ^{:key (:id job)}
           [job-card job {:logged-in?        logged-in?
                          :user-has-applied? has-applied?
                          :user-is-company?  (not (nil? company-id))
                          :user-is-owner?    (or admin? (= company-id (:company-id job)))
                          :apply-source      "company-jobs-job"}])
         (nil? jobs)
         [:h3 "Loading..."]
         :else
         [:h3 "No jobs found."])]
      (when (and (not-empty jobs)
                 (> (<sub [::subs/total-number-of-jobs]) jobs/page-size))
        [pagination/pagination
         current-page
         (pagination/generate-pagination current-page (<sub [::subs/number-of-pages]))
         :company-jobs
         (<sub [:wh/query-params])
         {:slug (<sub [::subs/company-slug])}])]]))
