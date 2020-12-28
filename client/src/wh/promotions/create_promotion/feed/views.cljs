(ns wh.promotions.create-promotion.feed.views
  (:require [wh.common.url :as url]
            [wh.landing-new.views :as landing-views]
            [wh.logged-in.profile.components :refer [text-field]]
            [wh.promotions.create-promotion.components :as components]
            [wh.promotions.create-promotion.events :as events]
            [wh.promotions.create-promotion.subs :as subs]
            [wh.re-frame.subs :refer [<sub]]
            [wh.styles.promotions :as styles]
            [wh.util :as util]))

(defn- description []
  [:div
   [:label
    [:h3 "Enter promotion description"]
    [text-field (<sub [::subs/description])
     {:on-change   [::events/edit-description]
      :placeholder "Great opportunity to learn new skills…"
      :data-test   "enter-promotion-description"
      :auto-focus  true
      :class       styles/create-promotion__description}]]])

(defn- preview []
  (let [object      (<sub [::subs/object])
        promoter    (<sub [::subs/promoter])
        vertical    (<sub [:wh/vertical])
        environment (<sub [:wh/env])]
    [:div
     [:h3 "Promotion preview"]

     (cond
       (= object :not-allowed)
       [:h4 "The object you're trying to promote is not public right now. You cannot promote it."]

       (= object :not-recognized)
       [:h4 "Something wrong happened. Please contact dev-team"]

       (map? object)
       [landing-views/activity-card
        {:verb        :promote
         :object-type (<sub [::subs/object-type])
         :description (<sub [::subs/description])
         :object      object
         :actor       promoter}
        {:logged-in? true
         :base-uri   (url/vertical-homepage-href environment vertical)
         :vertical   vertical}])]))

(defn promote []
  [:div (util/smc styles/create-promotion)
   [description]

   [preview]

   [components/send :feed "Publish Promotion to Feed"]])
