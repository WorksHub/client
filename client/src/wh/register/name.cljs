(ns wh.register.name
  (:require [re-frame.core :refer [dispatch dispatch-sync]]
            [wh.components.conversation.views :refer [codi-message error-message button]]
            [wh.components.forms.views :refer [target-checked]]
            [wh.register.events :as events]
            [wh.register.subs :as subs]
            [wh.subs :as subs-common :refer [<sub]]))

(defn name-box []
  [:div.animatable
   [:div.conversation-element.user
    [:label.label {:for "name"} "Your name"]
    [:input#name {:type       :text
                  :auto-focus true
                  :value      (<sub [::subs/name])
                  :on-change  #(dispatch-sync [::events/set-name (-> % .-target .-value)])
                  :aria-label "Name input"
                  :placeholder "e.g. John Snow"}]]])

(defn panel []
  [:form
   {:on-submit #(do (.preventDefault %)
                    (dispatch [::events/proceed-from-name]))}
   [codi-message
    (if (<sub [::subs/preset-name?])
      "Is this your full name?"
      "Please enter your full name")]
   [name-box]
   [button "Next" nil]
   (when-let [error (<sub [::subs/upsert-user-errors])]
     [error-message error])])
