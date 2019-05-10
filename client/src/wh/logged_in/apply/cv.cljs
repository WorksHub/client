(ns wh.logged-in.apply.cv
  (:require
    [wh.common.upload :as upload]
    [wh.components.conversation.views :refer [codi-message error-message]]
    [wh.logged-in.apply.events :as events]
    [wh.logged-in.apply.subs :as subs]
    [wh.logged-in.profile.events :as profile-events]
    [wh.subs :refer [<sub]]
    [wh.user.subs :as user-subs]))

(defn upload-button []
  [:label.file-label.animatable
   [:input.file-input {:id "application-bot_upload-cv"
                       :type      "file"
                       :name      "avatar"
                       :on-change (upload/handler
                                    :launch [::profile-events/cv-upload]
                                    :on-upload-start [::events/cv-upload-start]
                                    :on-success [::events/cv-upload-success]
                                    :on-failure [::events/cv-upload-failure])}]
   [:span.file-cta.conversation-button
    [:span.file-label "Upload resume"]]])

(defn panel []
  [:div.cv-upload
   (if (<sub [::subs/cv-upload-failed?])
     [error-message "There was an error uploading your resume, please try again."]
     [codi-message "\uD83D\uDC4B " (<sub [::user-subs/name])
      " - Codi here, as this is your first application could you please upload your resume or CV?"])
   [upload-button]])
