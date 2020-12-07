(ns wh.components.side-card.side-card-mobile
  (:require [wh.components.common :refer [link wrap-img img]]
            [wh.components.icons :as icons]
            [wh.interop :as interop]
            [wh.routes :as routes]
            [wh.styles.side-card-mobile :as style]
            [wh.util :as util]))

(defn connected-entity [entity]
  [:div (util/smc style/connected-entity)
   (wrap-img img
             (or (:logo entity) (:image-url entity))
             {:w 16 :h 16 :class style/connected-entity__avatar})
   [:span (util/smc style/connected-entity__title)
    (:name entity)]])

(defn title
  ([text]
   [title text :default])
  ([text type]
   [:div (util/smc style/title
                   (when (= type :extended) style/title--extended))
    text]))

(defn view-button []
  [:div (util/smc style/button)
   "View"])

(defn blog-card [blog]
  [:a {:class style/card
       :href  (routes/path :blog :params {:id (:id blog)})}
   [:div (util/smc style/card__content)
    [title (:title blog) :extended]
    [connected-entity (:author-info blog)]]
   [view-button]])

(defn job-card [{:keys [slug company-info] :as job}]
  [:a {:class style/card
       :href  (routes/path :job :params {:slug slug})}
   [:div (util/smc style/card__content)
    [connected-entity company-info]
    [title (:title job)]]
   [view-button]])

(defn issue-card [issue]
  [:a {:class style/card
       :href  (routes/path :issue :params {:id (:id issue)})}
   [:div (util/smc style/card__content)
    [connected-entity (:company issue)]
    [title (:title issue) :extended]]
   [view-button]])
