(ns wh.logged-in.apply.thanks
  (:require
    [re-frame.core :refer [dispatch]]
    [wh.components.common :refer [link]]
    [wh.components.conversation.views :refer [codi-message error-message button]]
    [wh.logged-in.apply.events :as events]
    [wh.logged-in.apply.subs :as subs]
    [wh.subs :refer [<sub]]))

(defn panel []
  (if (<sub [::subs/submit-success?])
    [:div
     [codi-message "Good news \uD83D\uDC4F, your application was successful!"]
     [codi-message "Your dedicated Talent Manager will be in touch to discuss next steps."]
     [codi-message "In the meantime check some other great jobs we have "
      [link "recommended" :recommended :class "a--underlined" :on-click #(dispatch [::events/track-recommendations-redirect])]
      " for you." ]]
    [:div
     [error-message (<sub [::subs/error-message])]
     (when (= (<sub [::subs/error]) :incomplete-profile)
       [link [button "Edit Profile"] :profile :on-click #(dispatch [::events/close-chatbot])])
     [button "Re-try submit" [::events/apply]]]))
