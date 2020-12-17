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
   [:label
    [:h3 "Enter promotion description"]
    [components/text-field (<sub [::subs/description])
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

(defn- status []
  (let [promotion-status (<sub [::subs/send-promotion-status])]
    (case promotion-status
      :success [:h3 (util/smc styles/create-promotion__send__status) "Promotion is published!"]
      :failure [:h3 (util/smc styles/create-promotion__send__status)
                "We couldn't publish your promotion. Please try again. If problem persists contact dev-team"]
      nil)))

(defn- send []
  (let [description      (<sub [::subs/description])
        object-type      (<sub [::subs/object-type])
        object-id        (<sub [::subs/object-id])
        can-publish?     (<sub [::subs/can-publish?])
        promotion-status (<sub [::subs/send-promotion-status])]
    [:div (util/smc styles/create-promotion__send)
     [status]

     [:a.button.button--medium
      {:class    (util/mc styles/create-promotion__send__button)
       :disabled (not can-publish?)
       :on-click #(when can-publish?
                    (dispatch [::events/send-promotion!
                               {:description description
                                :object-type object-type
                                :object-id   object-id}]))}
      "Publish Promotion!"]]))

(defn page []
  [:div.main
   [:div (util/smc styles/create-promotion)
    [header]

    [description]

    [preview]

    [send]]])
