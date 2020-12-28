(ns wh.promotions.create-promotion.components
  (:require [re-frame.core :refer [dispatch]]
            [wh.promotions.create-promotion.subs :as subs]
            [wh.re-frame.subs :refer [<sub]]
            [wh.promotions.create-promotion.events :as events]
            [wh.routes :as routes]
            [wh.styles.promotions :as styles]
            [wh.util :as util]))

(defn- status [promotion-status]
  (case promotion-status
    :success [:h3 (util/smc styles/create-promotion__send__status) "Promotion is published!"]
    :failure [:h3 (util/smc styles/create-promotion__send__status)
              "We couldn't publish your promotion. Please try again. If problem persists contact dev-team"]
    nil))

(defn send [channel text]
  (let [description      (<sub [::subs/description])
        object-type      (<sub [::subs/object-type])
        object-id        (<sub [::subs/object-id])
        can-publish?     (<sub [::subs/can-publish? channel])
        promotion-status (<sub [::subs/send-promotion-status channel])]
    [:div (util/smc styles/create-promotion__send)
     [status promotion-status]

     [:a.button.button--medium
      {:class    (util/mc styles/create-promotion__send__button)
       :disabled (not can-publish?)
       :on-click #(when can-publish?
                    (dispatch [::events/send-promotion!
                               {:description description
                                :object-type object-type
                                :channel     channel
                                :object-id   object-id}]))}
      text]]))
