(ns wh.register.location
  (:require [wh.components.conversation.views :refer [button codi-message]]
            [wh.register.conversation :refer [input-with-suggestions]]
            [wh.register.db :as db]
            [wh.register.events :as events]
            [wh.register.subs :as subs]
            [wh.subs :refer [<sub]]))

(defn location-box [type]
  [input-with-suggestions
   :input-sub [::subs/location-query type]
   :set-input-ev [::events/set-location-query type]
   :error-sub [::subs/location-search-error type]
   :dropdown-sub [::subs/location-search-results type]
   :set-dropdown-ev [::events/set-location type]])

(defn panel []
  [:div.multiple-conversations
   (when (<sub [::subs/location-stage-visible? :confirm-preferred-location])
     [:div
      [codi-message "It looks like you're in " [:span.location (<sub [::subs/preset-city])] ", is this where you would be looking for jobs?"]
      [button "Yes, I'm job hunting here" [:register/confirm-preferred-location]]
      [button "No, but I can tell you where I'd like to work" [:register/ask-for-preferred-location]]
      [button "I prefer remote work" [:register/toggle-prefer-remote]
       :class (when (<sub [::subs/remote]) "conversation-button--selected")]])
   (when (<sub [::subs/location-stage-visible? :ask-for-preferred-location])
     [:div
      [codi-message "So tell me, where do you want to work?"]
      [location-box :preferred]
      (when-not (<sub [::subs/location-stage-visible? :confirm-preferred-location])
        [button "I prefer remote work" [:register/toggle-prefer-remote]
         :class (when (<sub [::subs/remote]) "conversation-button--selected")])])
   (when (<sub [::subs/location-stage-visible? :confirm-current-location])
     [:div
      [codi-message "Is " [:span.location (<sub [::subs/preferred-city])] " your current location?"]
      [button "Yes" [:register/confirm-current-location]]
      [button "Enter current location" [:register/ask-for-current-location]]])
   (when (<sub [::subs/location-stage-visible? :ask-for-current-location])
     [:div
      [codi-message "Please enter your current location."]
      [location-box :current]])])
