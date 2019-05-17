(ns wh.logged-in.apply.views
  (:require
    [re-frame.core :refer [dispatch dispatch-sync]]
    [reagent.core :as r]
    [wh.common.upload :as upload]
    [wh.common.user :as user]
    [wh.components.common :refer [link]]
    [wh.components.conversation.views :refer [codi-message error-message button]]
    [wh.components.forms.views :refer [labelled-checkbox multiple-checkboxes select-field text-input text-field radio-buttons]]
    [wh.components.icons :refer [icon]]
    [wh.logged-in.apply.events :as events]
    [wh.logged-in.apply.subs :as subs]
    [wh.logged-in.profile.events :as profile-events]
    [wh.subs :refer [<sub]]
    [wh.user.subs :as user-subs]
    [wh.views]))

(defn add-full-name-step []
  (let [name (r/atom (<sub [::user-subs/name]))
        validation-error? (r/atom false)]
    (fn []
      [:div.add-full-name
       (if (<sub [::subs/update-name-failed?])
         [error-message "There was an error updating your name, please try again."]
         [codi-message "What is your full name?"])
       [:div.animatable
        [:div.conversation-element.user
         [:input {:type       :text
                  :auto-focus true
                  :value      @name
                  :on-change  #(do (reset! name (-> % .-target .-value))
                                   (reset! validation-error? false))
                  :aria-label "Name input"}]]]
       [:div.animatable
        [:button.conversation-button.update-name
         (when (<sub [::subs/current-step? :name])
           {:class    (when (<sub [::subs/updating?]) "button--loading")
            :id       "application-bot_update-name"
            :on-click #(if (user/full-name? @name)
                         (dispatch [::events/update-name @name])
                         (reset! validation-error? true))})
         "Next"]]
       (when @validation-error?
         [:div.conversation-button--error "Please enter at least two words"])])))

(defn cv-upload-step []
  (let [current-step? (<sub [::subs/current-step? :cv-upload])]
    [:div.cv-upload
     (if (<sub [::subs/cv-upload-failed?])
       [error-message "There was an error uploading your resume, please try again."]
       [codi-message "Could you please upload your resume or CV?"])
     [:label.file-label.animatable
      [:input.file-input {:id        "application-bot_upload-cv"
                          :type      "file"
                          :name      "avatar"
                          :on-change (when current-step?
                                       (upload/handler
                                         :launch [::profile-events/cv-upload]
                                         :on-upload-start [::events/cv-upload-start]
                                         :on-success [::events/cv-upload-success]
                                         :on-failure [::events/cv-upload-failure]))}]
      [:span.file-cta.conversation-button
       [:span.file-label {:class (when (and current-step?
                                            (<sub [::subs/updating?]))
                                   "button--loading")}
        "Upload resume"]]]]))

(defn thanks-step []
  (if (<sub [::subs/submit-success?])
    [:div
     [codi-message "Good news \uD83D\uDC4F, your application was successful!"]
     [codi-message "Your dedicated Talent Manager will be in touch to discuss next steps."]
     [codi-message "In the meantime check some other great jobs we have "
      [link "recommended" :recommended :class "a--underlined" :on-click #(dispatch [::events/track-recommendations-redirect])]
      " for you."]]
    [:div
     [error-message (<sub [::subs/error-message])]
     (when (= (<sub [::subs/error]) :incomplete-profile)
       [link [button "Edit Profile"] :profile :on-click #(dispatch [::events/close-chatbot])])
     [button "Re-try submit" [::events/apply]]]))

(defn pre-application []
  [:div.multiple-conversations
   [:div
    [codi-message "Oh, snap! \uD83E\uDD14 We need a few more details about you"]]
   (when (and (<sub [::subs/step-taken? :name]))
     [add-full-name-step])
   (when (and (<sub [::subs/step-taken? :cv-upload]))
     [cv-upload-step])])


(defn chatbot []
  [:div.chatbot
   [icon "close"
    :class "close is-pulled-right"
    :id "application-bot_close-bot"
    :on-click #(dispatch [::events/close-chatbot])]
   (if (<sub [::subs/current-step? :thanks])
     [thanks-step]
     [pre-application])])

(defn overlay-apply []
  (when (<sub [::subs/display-chatbot?])
    [chatbot]))

(swap! wh.views/extra-overlays conj [overlay-apply])
