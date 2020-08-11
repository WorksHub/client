(ns wh.logged-in.apply.views
  (:require [re-frame.core :refer [dispatch dispatch-sync]]
            [reagent.core :as r]
            [wh.common.data :as data]
            [wh.common.upload :as upload]
            [wh.common.user :as user]
            [wh.components.common :refer [link]]
            [wh.components.conversation.views :refer [codi-message error-message button]]
            [wh.components.forms.views :refer
             [labelled-checkbox multiple-checkboxes select-field text-input
              text-field radio-buttons]]
            [wh.components.icons :refer [icon]]
            [wh.logged-in.apply.events :as events]
            [wh.logged-in.apply.subs :as subs]
            [wh.logged-in.profile.events :as profile-events]
            [wh.subs :refer [<sub]]
            [wh.user.subs :as user-subs]
            [wh.util :as util]
            [wh.views]))

(defn add-full-name-step []
  (let [full-name (r/atom (<sub [::user-subs/name]))
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
                  :value      @full-name
                  :on-change  #(do (reset! full-name (-> % .-target .-value))
                                   (reset! validation-error? false))
                  :aria-label "Name input"}]]]
       [:div.animatable
        [:button.conversation-button.update-name
         (when (= :step/name (<sub [::subs/current-step]))
           {:class    (when (<sub [::subs/updating?]) "button--loading")
            :id       "application-bot_update-name"
            :on-click #(if (user/full-name? @full-name)
                         (dispatch [::events/update-name @full-name])
                         (reset! validation-error? true))})
         "Next"]]
       (when @validation-error?
         [:div.conversation-button--error "Please enter at least two words"])])))


(defn current-location-step []
  (fn []
    [:div.add-current-location
     (if (<sub [::subs/update-current-location-failed?])
       [error-message "There was an error updating your current location, please try again"]
       [codi-message "What is your current location?"])
     [:div.animatable
      [:div.conversation-element.user.current-location-input
       [text-field (<sub [::subs/current-location-label])
        {:suggestions          (<sub [::subs/current-location-suggestions])
         :on-change            [::events/edit-current-location]
         :on-select-suggestion [::events/select-current-location-suggestion]
         :placeholder          "Type to search..."}]]]
     [:div.animatable
      [:button.conversation-button.update-current-location
       (when (= :step/current-location (<sub [::subs/current-step]))
         {:class    (when (<sub [::subs/updating?]) "button--loading")
          :id       "application-bot_update-current-location"
          :on-click #(dispatch [::events/update-current-location])})
       "Next"]]]))


(defn cv-upload-step []
  (let [current-step? (= :step/cv-upload (<sub [::subs/current-step]))]
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
       [:span {:class (if (and current-step? (<sub [::subs/updating?]))
                        "button--loading"
                        "file-label")}
        "Upload resume"]]]]))

(defn rejection-step []
  [:div
   [codi-message (<sub [::subs/rejection-message])]
   [codi-message "Why don't you check some other great jobs we have "
    [link "recommended" :recommended :class "a--underlined" :on-click #(do
                                                                         (dispatch [::events/close-chatbot])
                                                                         (dispatch [::events/track-recommendations-redirect]))]
    " for you."]])

(defn thanks-step []
  (let [managed? (<sub [::subs/company-managed?])]
    (if (<sub [::subs/submit-success?])
      [:div
       [codi-message "Everything looks good with your application! \uD83C\uDF89"]
       [codi-message (if managed?
                       "Your dedicated Talent Manager will be in touch to discuss next steps."
                       (str "If " (<sub [::subs/company-name]) " are interested in progressing your application then you will hear from them directly. Good luck!"))]
       [codi-message "In the meantime check some other great jobs we have "
        [link "recommended" :recommended :class "a--underlined" :on-click #(do
                                                                             (dispatch [::events/close-chatbot])
                                                                             (dispatch [::events/track-recommendations-redirect]))]
        " for you."]]

      [:div
       [error-message (<sub [::subs/error-message])]
       (when (= (<sub [::subs/error]) :incomplete-profile)
         [link [button "Edit Profile"] :profile :on-click #(dispatch [::events/close-chatbot])])
       [button "Re-try submit" [::events/apply]]])))

(defn cover-letter-step []
  (let [managed?                    (<sub [::subs/company-managed?])
        ;; Access default cover letter from user profile
        default-cover-letter        (<sub [::user-subs/cover-letter])
        cover-letter-upload-failed? (<sub [::subs/cover-letter-upload-failed?])
        updating?                   (<sub [::subs/updating?])]

    [:div
     [codi-message "Thanks for your application!"]

     (if cover-letter-upload-failed?
       [error-message "We couldn't upload your cover letter. Do you want to try again?"]
       [codi-message "Do you want to add a cover letter?"])

     (when default-cover-letter
       [button "Send my default cover letter"
        [::events/set-cover-letter default-cover-letter]
        :disabled updating?])

     [:label.file-label.animatable
      [:input.file-input {:id        "application-bot_upload-cover-letter"
                          :type      "file"
                          :name      "avatar"
                          :disabled  updating?
                          :on-change (upload/handler
                                       :launch [::profile-events/cover-letter-upload]
                                       :on-upload-start [::events/cover-letter-upload-start]
                                       :on-success [::events/cover-letter-upload-success]
                                       :on-failure [::events/cover-letter-upload-failure])}]

      [:span.file-cta.conversation-button
       [:span {:class (if updating?
                        "button--loading"
                        "file-label")}
        (if cover-letter-upload-failed?
          "Sure, let me try again!"
          "Sure, I'd love to add one!")]]]

     [button "I'm good, thanks :)" [::events/discard-cover-letter]
      :disabled updating?]]))

(defn pre-application []
  (if (<sub [:user/rejected?])
    [:div.multiple-conversations
     [:div
      [codi-message "Unfortunately you're unable to apply for this role until your profile has been approved \uD83D\uDE41"]]]

    [:div.multiple-conversations
     [:div
      [codi-message "Oh, snap! \uD83E\uDD14 We need a few more details about you"]]
     (when (<sub [::subs/step-taken? :step/name])
       [add-full-name-step])
     (when (<sub [::subs/step-taken? :step/current-location])
       [current-location-step])
     (when (<sub [::subs/step-taken? :step/cv-upload])
       [cv-upload-step])]))

(defn visa-step []
  (let [selected-options (r/atom #{})
        other (r/atom nil)]
    (fn []
      [:div.add-visa
       (if (<sub [::subs/update-visa-failed?])
         [error-message "There was an error updating your visa, please try again."]
         [:div
          [codi-message "Thanks for applying! \uD83D\uDC4F "]
          [codi-message "In order to correctly process your application, could you add your visa information?"]])
       [:div.animatable.visa-options
        [:div.multiple-buttons
         (doall
           (for [option (sort data/visa-options)
                 :let [selected? (contains? @selected-options option)]]
             [:button.button.visa-option
              {:key      option
               :class    (when-not selected?
                           "button--light")
               :on-click #(swap! selected-options (if selected? disj conj) option)}
              option]))]]
       (when (contains? @selected-options "Other")
         [:div
          [:span.conversation-element--label "Please specify:"]
          [:div.conversation-element.user
           [:input {:type       :text
                    :auto-focus true
                    :value      @other
                    :on-change  #(do (reset! other (-> % .-target .-value)))
                    :aria-label "Name input"}]]])
       [:div.animatable
        [:button.conversation-button.update-visa
         {:class    (when (<sub [::subs/updating?]) "button--loading")
          :id       "application-bot_update-visa"
          :on-click #(dispatch [::events/update-visa @selected-options @other])}
         "Next"]]])))

(defn chatbot []
  [:div.chatbot
   [icon "close"
    :class "close is-pulled-right"
    :id "application-bot_close-bot"
    :on-click #(dispatch [::events/close-chatbot])]

   (case (<sub [::subs/current-step])
     :step/thanks       [thanks-step]
     :step/cover-letter [cover-letter-step]
     :step/rejection    [rejection-step]
     :step/visa         [visa-step]
     [pre-application])])

(defn overlay-apply []
  (when (and (<sub [::subs/display-chatbot?])
             (<sub [::subs/current-step]))
    [chatbot]))

;; TODO: find a way to remove extra-overlays from codebase and implement
;; overlays in more sane manner
(swap! wh.views/extra-overlays conj [overlay-apply])
