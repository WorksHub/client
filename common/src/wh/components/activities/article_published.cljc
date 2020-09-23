(ns wh.components.activities.article-published
  (:require [wh.common.url :as url]
            [wh.components.activities.components :as components]
            [wh.components.common :refer [link wrap-img img base-img]]
            [wh.components.icons :as icons]
            [wh.routes :as routes]
            [wh.styles.activities :as styles]
            [wh.util :as util]))

(defn article-time [{:keys [date reading-time]}]
  [:p (util/smc styles/article__time)
   (str date "  •  " (if (zero? reading-time) 1 reading-time) " min read")])

(defn article-boosts [upvotes]
  [:div (util/smc styles/article__boost)
   [:div (util/smc styles/article__boost-icon-wrapper)
    [icons/icon "rocketship" :class styles/article__boost-icon]]
   [:span upvotes]])

(defn image [{:keys [feature] :as blog}]
  (when feature
    [:div (util/smc styles/article__feature)
     (wrap-img img feature {:w 578 :h 154 :class styles/article__feature-img :crop "center"})]))

(defn card
  [{:keys [title tags id reading-time display-date upvote-count]
    :as   blog}
   actor type
   {:keys [base-uri vertical facebook-app-id]}]
  (let [author-name (:name actor)]
    [components/card type
     [image blog]
     [:div (util/smc styles/article__content-wrapper)
      [article-boosts upvote-count]
      [components/card-content
       [components/header
        [components/title
         {:href (routes/path :blog :params {:id id})}
         title]
        [components/entity-description :blog type]]
       [components/meta-row
        [components/author {:img (:image-url actor)
                            :name author-name
                            :id (:id actor)}]
        [article-time
         {:date display-date :reading-time reading-time}]]
       [components/tags tags]]]
     [components/footer :default
      (let [url (str (url/strip-path base-uri)
                     (routes/path :blog :params {:id id}))]
        [components/actions
         {:share-opts {:url             url
                       :id              id
                       :content-title   title
                       :content         (str "'" title "'" (if (= type :publish) ", a brand new " ", an ") "article from " author-name)
                       :vertical        vertical
                       :facebook-app-id facebook-app-id}}])
      [components/footer-buttons
       [components/button
        {:href (routes/path :learn)
         :type :inverted}
        "All articles"]
       [components/button
        {:href (routes/path :blog :params {:id id})}
        "View article"]]]]))
