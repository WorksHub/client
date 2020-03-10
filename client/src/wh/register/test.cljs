(ns wh.register.test
  (:require [re-frame.core :refer [dispatch dispatch-sync subscribe]]
            [wh.components.conversation.views :refer [button codi-message error-message]]
            [wh.register.conversation :refer [next-button]]
            [wh.register.events :as events]
            [wh.register.subs :as subs]
            [wh.subs :refer [<sub] :as wh-subs]))

(defn code-input []
  [:div.animatable
   [:form.conversation-element.user
    {:on-submit #(do (.preventDefault %)
                     (when (not (<sub [::subs/blank-code-answer?]))
                       (dispatch [::events/check-code-riddle])))}
    [:label.label {:for "code"} "Corrected code"]
    [:input#code {:type       :text
                  :auto-focus true
                  :value      (<sub [::subs/code-answer])
                  :on-change  #(dispatch-sync [::events/set-code-answer (-> % .-target .-value)])
                  :aria-label "Code answer input"
                  :placeholder "Code without an error"}]]])

;TODO how to skip?
(defn panel []
  (cond
    (<sub [::subs/fetch-riddles-error])
    [:div
     [error-message "There was an error fetching data, please try again later"]]
    (<sub [::subs/approval-fail?])
    [:div
     [codi-message (str (when-not (<sub [::wh-subs/blockchain?])
                          "Unfortunately, that's not the right answer. ")
                        "Someone will have to review your profile before you have approved access to full job information.")]
     [codi-message "In the meantime you can add more detail to your profile which will help get you approved faster!"]
     [button "Continue" [:register/advance]]]
    :otherwise
    [:div
     [codi-message "The following code is incorrect"]
     [codi-message [:code (<sub [::subs/selected-riddle-code])]]
     [codi-message [:span.highlight "Correct this code"] " by re-typing it below"]
     (when (<sub [::subs/failed-code-riddle-check?])
       [error-message "That was not a correct answer, please try again"])
     [code-input]
     [button "Check" [::events/upsert-user]
      :disabled (<sub [::subs/blank-code-answer?])]
     [codi-message "We’re showing you a " [:span.highlight (<sub [::subs/selected-riddle-language])] " test. Would you like to select an alternative coding language?"]
     [:div.animatable
      (into [:select.conversation-button
             {:on-change #(dispatch-sync [::events/change-riddle (-> % .-target .-value)])}]
            (concat [[:option "Yes, show me the options"]]
                    (mapv (fn [language] [:option language]) (<sub [::subs/all-riddles-languages]))))]]))
