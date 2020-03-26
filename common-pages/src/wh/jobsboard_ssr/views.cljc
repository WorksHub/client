(ns wh.jobsboard-ssr.views
  (:require
    [wh.components.icons :refer [icon]]
    [wh.components.job :as job]
    [wh.routes :as routes]
    [wh.components.pagination :as pagination]
    [wh.interop :as interop]
    [wh.jobsboard-ssr.subs :as subs]
    [wh.re-frame.subs :refer [<sub]]))

(def jobs-container-class
  {:cards "jobs-board__jobs-list__content"
   :list  ""})

(def view-types
  {:view/list  {:icon-name "applications" :label "List"}
   :view/cards {:icon-name "jobs-board" :label "Cards"}})

(defn list-view-type [route query-params view-type]
  [:div.search__box.search__view-type.search__box-no-margin.search__view-type--ssr
   [:span.label "View: "]
   (for [[type {:keys [label icon-name] :as value}] view-types]
     (let [selected (= (name type) (name view-type))]
       ^{:key type}
       [:a
        {:href  (routes/path route
                             :query-params (assoc query-params "view-type" (name type)))
         :class (if selected
                  "search__view-type__button search__view-type__button--selected"
                  "search__view-type__button")}
        [icon icon-name]
        [:span label]]))])

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

(defn jobs-board [route query-params view-type]
  (let [jobs         (<sub [::subs/jobs])
        current-page (<sub [::subs/current-page])
        total-pages  (<sub [::subs/total-pages])
        logged-in?   (<sub [:user/logged-in?])
        company-id   (<sub [:user/company-id])
        admin?       (<sub [:user/admin?])
        has-applied? (some? (<sub [:user/applied-jobs]))]
    [:section.jobs-board__jobs-list.jobs-board__jobs-list--ssr
     [:div
      {:class (jobs-container-class view-type)}
      (for [job jobs]
        ^{:key (str "col-" (:id job))}
        [job/job-card job {:logged-in?        logged-in?
                           :view-type         view-type
                           :user-has-applied? has-applied?
                           :user-is-company?  (or admin? (= company-id (:company-id job)))
                           :user-is-owner?    (= company-id (:company-id job))
                           :apply-source      "jobsboard-job"}])]

     (when (seq jobs)
       [pagination/pagination current-page (pagination/generate-pagination current-page total-pages) route query-params])]))

(defn jobsboard-page []
  [:div.main.jobs-board
   [header]
   [:div.search-results
    (if (<sub [::subs/all-jobs?])
      [:h2 "All Jobs"]
      [:h3.search-result-count (<sub [::subs/search-result-count-str])])
    (let [view-type    (<sub [::subs/view-type])
          query-params (<sub [:wh/query-params])]
      [:section
       [list-view-type :jobsboard query-params view-type]
       [jobs-board :jobsboard query-params view-type]])]])

(defn preset-search-page []
  (let [view-type (<sub [::subs/view-type])]
    [:div.main.jobs-board__pre-set-search
     [header]
     (let [query-params (<sub [:wh/query-params])]
       [:div.search-results
        [:h3.search-result-count (<sub [::subs/search-result-count-str])]
        [:section
         [jobs-board :preset-search query-params view-type]]])]))
