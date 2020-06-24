(ns wh.components.activities.article-published
  (:require [wh.components.activities.components :as components]
            [wh.components.icons :as icons]
            [wh.routes :as routes]
            [wh.styles.activities :as styles]
            [wh.util :as util]))

(defn article-time [{:keys [date reading-time]}]
  [:p (util/smc styles/article__time)
   (str date "  •  " reading-time " min read")])

(defn article-boosts [upvotes]
  [:div (util/smc styles/article__boost)
   [:div (util/smc styles/article__boost-icon-wrapper)
    [icons/icon "rocketship" :class styles/article__boost-icon]]
   [:span (count upvotes)]])

(defn image [{:keys [feature] :as blog}]
  (when feature
    [:div (util/smc styles/article__feature)
     [:img {:class (util/mc styles/article__feature-img)
            :src   feature}]]))

(defn card [{:keys [title tags id author
                    author-info reading-time display-date upvotes]
             :as   blog}]
  [components/card
   [image blog]
   [:div (util/smc styles/article__content-wrapper)
    [article-boosts upvotes]
    [components/card-content
     [components/header
      [components/title
       {:href (routes/path :blog :params {:id id})}
       title]
      [components/entity-icon "union"]]
     [components/meta-row
      [components/author {:img (:image-url author-info)} author]
      [article-time
       {:date display-date :reading-time reading-time}]]
     [components/tags tags]]]
   [components/footer :default
    [components/footer-buttons
     [components/button
      {:href (routes/path :learn)
       :type :inverted}
      "All articles"]
     [components/button
      {:href (routes/path :blog :params {:id id})}
      "View article"]]]])
