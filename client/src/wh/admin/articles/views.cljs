(ns wh.admin.articles.views
  (:require [clojure.string :as str]
            [wh.admin.articles.subs :as subs]
            [wh.components.common :refer [link]]
            [wh.components.pagination :as pagination]
            [wh.components.tag :as tag]
            [wh.subs :refer [<sub]]))

(defn article
  [{:keys [id title formatted-date author author-id author-info verticals company tags]}]
  (let [author-name (or (:name author-info) author)]
    [:div.admin-article
     [link [:h3 title] :blog :id id]
     [:div.admin-article__info
      [:span "Submitted: " formatted-date]
      [:span "Author: " (if author-id
                          [link author-name :candidates :id author-id :class "a--underlined"]
                          author-name)]
      [:span "Company: " (if company [link (:name company) :company :slug (:slug company) :class "a--underlined"] "n/a")]
      [:span "Verticals: " (str/join ", " verticals)]
      [tag/tag-list :li tags]]]))

(defn page
  []
  [:div.main.admin-articles
   [:h1 "Unpublished Articles"]
   (let [articles (<sub [::subs/articles])
         error?   (<sub [::subs/error?])]
     (cond error?
           [:h3.is-error "An error occurred! :("]
           (seq articles)
           [:div
            [:p "We should aim to keep the number of unpublished articles as low as possible. If there are any in this list then please contact the author or creator to determine what needs to happen to get it published ASAP."]
            (for [a (<sub [::subs/articles])]
              ^{:key (:id a)}
              [article a])
            (when-let [pagination (<sub [::subs/pagination])]
              [pagination/pagination
               (<sub [::subs/current-page-number])
               pagination
               (<sub [:wh/page])
               (<sub [:wh/query-params])
               (<sub [:wh/page-params])])]
           (coll? articles)
           [:h3 "No results :("]
           :else
           [:h3 "Loading..."]))])
