(ns wh.components.cards
  (:require
    [wh.common.text :refer [pluralize]]
    [wh.components.common :refer [link wrap-img blog-card-hero-img]]
    [wh.routes :as routes]
    [wh.slug :as slug]
    [wh.util :as util]
    #?(:cljs [wh.components.cards.subs])
    #?(:cljs [wh.subs :refer [<sub]])))

(defn blog-card
  [{:keys [id title feature author formatted-creation-date reading-time upvote-count tags creator published] :as blog}]
  (let [skeleton? (and blog (empty? (dissoc blog :id)))]
    [:div {:class (util/merge-classes "card"
                                      "card--blog"
                                      (str "i-cur-" (rand-int 9))
                                      (when skeleton? "blog-card--skeleton"))}
     (if skeleton?
       [:div.hero]
       (link [:div.hero
              (wrap-img blog-card-hero-img feature {:alt "Blog hero image"})] :blog :id id))
     (if skeleton?
       [:div.blog-info
        [:div.title]
        [:ul.tags
         [:li] [:li] [:li]]]
       [:div.blog-info
        (link [:div.title title] :blog :id id)
        (link [:div.author author] :blog :id id)
        (link [:div.datetime formatted-creation-date " | " reading-time " min read | " upvote-count " " (pluralize upvote-count "boost")] :blog :id id)
        (into [:ul.tags]
              (for [tag tags]
                [:li
                 [:a {:href (routes/path :learn-by-tag :params {:tag (slug/slug+encode tag)})}
                  tag]]))
        #?(:cljs
           [:div
            (when (<sub [:blog-card/can-edit? creator published])
              [link [:button.button.button--edit-blog "Edit"] :contribute-edit :id id])
            (when (<sub [:blog-card/show-unpublished? creator published])
              [:span.card__label.card__label--unpublished.card__label--blog "unpublished"])])])]))
