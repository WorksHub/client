(ns wh.promotions.create-promotion.views
  (:require [re-frame.core :refer [dispatch]]
            [wh.common.url :as url]
            [wh.landing-new.views :as landing-views]
            [wh.logged-in.profile.components :as components]
            [wh.promotions.create-promotion.events :as events]
            [wh.promotions.create-promotion.subs :as subs]
            [wh.re-frame.subs :refer [<sub]]
            [wh.styles.promotions :as styles]
            [wh.util :as util]))

(defn- header []
  [:div
   [:h1 "Post a promotion to the feed"]])

(defn- description []
  [:div
   [components/text-field (<sub [::subs/description])
    {:on-change   [::events/edit-description]
     :placeholder "Enter promotion description"
     :data-test   "enter-promotion-description"
     :class       styles/create-promotion__description}]])

(defn- preview []
  (let [object      (<sub [::subs/object])
        promoter    (<sub [::subs/promoter])
        vertical    (<sub [:wh/vertical])
        environment (<sub [:wh/env])]
    [:div
     [:h3 "Promotion preview"]

     [landing-views/activity-card
      {:verb        :promote
       :object-type (<sub [::subs/object-type])
       :description (<sub [::subs/description])
       :object      object
       :actor       promoter}
      {:logged-in? true
       :base-uri   (url/vertical-homepage-href environment vertical)
       :vertical   vertical}]]))

(defn- send []
  [:div (util/smc styles/create-promotion__send)
   [:a.button.button--medium
    {:class    (util/mc styles/create-promotion__send__button)
     :on-click #(dispatch [::events/send-promotion!])}
    "Publish Promotion!"]])

(defn page []
  [:div.main
   [:div (util/smc styles/create-promotion)
    [header]

    [description]

    [preview]

    [send]]])
