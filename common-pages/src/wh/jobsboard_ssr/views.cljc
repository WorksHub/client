(ns wh.jobsboard-ssr.views
  (:require
    [wh.components.job :as job]
    [wh.components.icons :refer [icon]]
    [wh.components.pagination :as pagination]
    [wh.interop :as interop]
    [wh.jobsboard-ssr.subs :as subs]
    [wh.re-frame.subs :refer [<sub]]))

(defn header []
  (let [{:keys [title subtitle description]} (<sub [::subs/header-info])]
    [:div.jobs-board__header
     [:div
      [:h1 title]
      [:h2 subtitle]
      [:h3 description]
      [:a.jobs-board__header__filter-toggle.a--capitalized-red.a--hover-red
       (interop/on-click-fn
        (interop/show-auth-popup :search-jobs [:jobsboard]))
       [:div
        [icon "plus" :class "search__toggle"]
        [:span "Show filters"]]]]]))

(defn jobs-board [route]
  (let [jobs (<sub [::subs/jobs])
        current-page (<sub [::subs/current-page])
        total-pages (<sub [::subs/total-pages])
        query-params (<sub [:wh/query-params])]
    [:section
     (when-let [parts (seq (partition-all 3 jobs))]
       [:div
        (doall
         (for [part parts]
           [:div.columns {:key (hash part)}
            (doall
             (for [job part]
               [:div.column.is-4 {:key (str "col-" (:id job))}
                [job/job-card job {:public? (not (<sub [:user/logged-in?]))}]]))]))])
     (when (seq jobs)
       [pagination/pagination current-page (pagination/generate-pagination current-page total-pages) route query-params])]))

(defn jobsboard-page []
  [:div.main.jobs-board
   [header]
   [:div.search-results
    (if (<sub [::subs/all-jobs?])
      [:h2 "All Jobs"]
      [:h3.search-result-count (<sub [::subs/search-result-count-str])])
    [:section
     [jobs-board :jobsboard]]]])

(defn preset-search-page []
  [:div.main.jobs-board__pre-set-search
   [header]
   [:div.search-results
    [:h3.search-result-count (<sub [::subs/search-result-count-str])]
    [:section
     [jobs-board :preset-search]]]])
