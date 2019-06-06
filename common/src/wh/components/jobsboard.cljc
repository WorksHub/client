(ns wh.components.jobsboard
  (:require
    [wh.components.job :as job]
    [wh.components.icons :refer [icon]]
    [wh.components.pagination :as pagination]
    [wh.interop :as interop]))

(defn header [{:keys [title subtitle description]}]
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
      [:span "Show filters"]]]]])

(defn jobs-board [{:keys [user jobs current-page route total-pages query-params]}]
  [:section
   (when-let [parts (seq (partition-all 3 jobs))]
     [:div
      (doall
        (for [part parts]
          [:div.columns {:key (hash part)}
           (doall
             (for [job part]
               [:div.column.is-4 {:key (str "col-" (:id job))}
                [job/job-card job {:public? (not user)}]]))]))])
   (when (seq jobs)
     [pagination/pagination current-page (pagination/generate-pagination current-page total-pages) route query-params])])

(defn jobsboard-page [{:keys [user jobs current-page total-pages query-params all-jobs? search-result-count-str header-params]}]
  [:div.main.jobs-board
   [header header-params]
   [:div.search-results
    (if all-jobs?
      [:h2 "All Jobs"]
      [:h3.search-result-count search-result-count-str])
    [:section
     [jobs-board {:user         user
                  :jobs         jobs
                  :current-page current-page
                  :route        :jobsboard
                  :total-pages  total-pages
                  :query-params query-params}]]]])

(defn preset-search-page [{:keys [user jobs search-result-count-str current-page total-pages header-params]}]
  [:div.main.jobs-board__pre-set-search
   [header header-params]
   [:div.search-results
    [:h3.search-result-count search-result-count-str]
    [:section
     [jobs-board {:user         user
                  :jobs         jobs
                  :current-page current-page
                  :route        :preset-search
                  :total-pages  total-pages}]]]])