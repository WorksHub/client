(ns wh.company.articles.views
  (:require
    [clojure.string :as str]
    [wh.company.articles.db :as articles]
    [wh.company.articles.subs :as subs]
    [wh.company.listing.views :refer [company-card]]
    [wh.components.cards :refer [blog-row]]
    [wh.components.pagination :as pagination]
    [wh.re-frame.subs :refer [<sub]]))

(defn page
  []
  (let [logged-in?   (<sub [:user/logged-in?])
        company-id   (<sub [:user/company-id])
        admin?       (<sub [:user/admin?])
        articles     (<sub [::subs/articles])
        current-page (<sub [::subs/page-number])]
    [:div.main.company-articles
     [company-card (<sub [::subs/company-card])]
     (if (not-empty articles)
       [:h1 (str "All Articles from " (<sub [::subs/name]))]
       [:h3 (str (<sub [::subs/name]) " has not posted any articles yet \uD83D\uDE15")])
     (when (not-empty articles)
       [:div.company-articles__grid-list
        (for [article articles]
          ^{:key (:id article)}
          [blog-row article])])
     (when (and (not-empty articles) (> (<sub [::subs/total-number-of-articles]) articles/page-size))
       [pagination/pagination
        current-page
        (pagination/generate-pagination current-page (<sub [::subs/number-of-pages]))
        :company-articles
        (<sub [:wh/query-params])
        {:slug (<sub [::subs/company-slug])}])]))
