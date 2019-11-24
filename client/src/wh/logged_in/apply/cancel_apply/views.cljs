(ns wh.logged-in.apply.cancel-apply.views
  (:require [wh.components.conversation.views :refer [codi-message error-message button]]
            [wh.components.icons :refer [icon]]
            [wh.components.common :refer [link]]
            [wh.logged-in.apply.events :as apply-events]
            [wh.logged-in.apply.cancel-apply.events :as events]
            [wh.logged-in.apply.subs :as apply-subs]
            [wh.logged-in.apply.cancel-apply.subs :as subs]
            [wh.views]
            [wh.subs :refer [<sub]]
            [reagent.core :as r]
            [re-frame.core :refer [dispatch]]))

;; the "x" will not show if the user is not logged-in, and we can assume the user has a complete profile,
;; as they have applied. We only need to take a user's reason, whatever it is and add it to ::cancel-apply/reason
;; within that sub-db, and then try and push that change. If it fails , let them try again, otherwise it is done
;; and we can say thanks.

(defn reason-step
  []
  (let [reason (r/atom "")
        validation-error? (r/atom false)]
    (fn []
      [:div.give-reason
       (if (<sub [::subs/reason-failed?])
         [error-message "There was an error , please try again."]
         [codi-message "Please tell us why you would like to cancel."])
       [:div.animatable
        [:button.conversation-button
         (when (= :reason (<sub [::subs/current-step]))
           {:class   (when (= "found work" (<sub [::subs/reason])) "button--has-reason")
            :id       "application-bot_update-reason"
            :on-click #(dispatch [::events/set-reason "found work"])})
         "I've found work"]]
       [:div.animatable
        [:button.conversation-button
         (when (= :reason (<sub [::subs/current-step]))
           ;; replace button loading with just a different colour,
           ;; form will be submitted by the submit btn , not these ones.
           {:class    (when (= "changed mind" (<sub [::subs/reason])) "button--has-reason")
            :id       "application-bot_update-reason"
            :on-click #(dispatch [::events/set-reason "changed mind"])})
         "I've changed my mind"]]
       [:div.animatable
        [:div.conversation-element.user
         [:input {:type :text
                  :auto-focus true
                  :placeholder "other"
                  ;; reason is initially nil so placeholder is informative
                  :value @reason
                  :on-change  #(do (reset! reason (-> % .-target .-value))
                                   (dispatch [::events/set-reason @reason])
                                   (reset! validation-error? false))
                  :aria-label "Reason input"}]]]
       [:div.animatable
        [:button.conversation-button
         (when (= :reason (<sub [::subs/current-step]))
           {:class    (when (<sub [::subs/updating?]) "button--loading")
            :id       "application-bot_update-reason"
            :on-click #(if (events/acceptable-reason? @reason)
                         (dispatch [::events/cancel-application])
                         (reset! validation-error? true))})
         "Submit"]]
       ;; not sure yet what will spring this up ...
       (when @validation-error?
         [:div.conversation-button--error "There was a problem validating this reason..."])])))

(defn thanks-step
  []
  (if (<sub [::subs/submit-success?])
    [:div
     [codi-message "Your cancellation has been processed."]
     [codi-message "The job has been returned to your "
      [link "recommended" :recommended :class "a--underlined" :on-click  #(do
                                                                            (dispatch [::events/close-chatbot])
                                                                            (dispatch [::apply-events/track-recommendations-redirect]))] ", in case you want to reapply in the future."]]
    [:div
     [error-message (<sub [::subs/reason-failed?])]
     [button "Re-try cancellation" [::events/start-cancellation]]]))

(defn chatbot
  []
  [:div.chatbot
   [icon "close"
    :class "close is-pulled-right"
    :id "application-bot_close-bot"
    :on-click #(dispatch [::events/close-chatbot])]
   (case (<sub [::subs/current-step])
     :reason [reason-step]
     :thanks [thanks-step])])

(defn overlay-cancel-apply
  []
  (when (and (<sub [::subs/display-chatbot?])
             (<sub [::subs/current-step]))
    [chatbot]))

(swap! wh.views/extra-overlays conj [overlay-cancel-apply])


