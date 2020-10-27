(ns wh.profile.update-public.views
  (:require [wh.components.modal :as modal]
            [wh.common.upload :as upload]
            [wh.profile.update-public.subs :as subs]
            [wh.profile.update-public.events :as events]
            [wh.components.forms.views :as forms]
            [wh.styles.profile :as styles]
            [wh.subs :refer [<sub]]
            [re-frame.core :refer [dispatch]]))

(defn content []
  [:div {:class styles/modal-content}
   [forms/avatar-field
    {:uploading-avatar? (<sub [::subs/avatar-uploading?])
     :avatar-url (<sub [::subs/avatar])
     :set-avatar (upload/handler
                   :launch [::events/avatar-upload]
                   :on-upload-start [::events/avatar-upload-start]
                   :on-success [::events/avatar-upload-success]
                   :on-failure [::events/avatar-upload-failure])}]
   [:div {:class styles/short-wrapper}
    [forms/text-input-with-label
     (<sub [::subs/name])
     {:label "Name"
      :required? true
      :placeholder "Name"
      :on-change [::events/edit-name]}]
    [forms/error-component (<sub [::subs/field-error :name])]]
   [:div {:class styles/short-wrapper}
    [forms/multi-edit
     (<sub [::subs/urls])
     {:label         "Your website"
      :placeholder   "Github, personal site, etc."
      :on-change     [::events/edit-url]
      :component     forms/text-input-with-label
      :lens          :url
      :class-wrapper styles/other-urls}]
    [forms/error-component (<sub [::subs/field-error :other-urls])]]
   [:div
    [forms/text-input-with-label
     (<sub [::subs/summary])
     {:label "Bio"
      :placeholder "Tell companies and our technical community a little bit about yourself"
      :type :textarea
      :on-change [::events/edit-summary]
      :rows 4}]
    [forms/error-component (<sub [::subs/field-error :summary])]]])

(defn profile-edit-modal []
  (let [open? (<sub [::subs/editing-profile?])
        on-close #(dispatch [::events/close-modal])]
    [modal/modal {:open? open?
                  :on-request-close on-close
                  :label "Edit your profile"}
     [modal/header {:title "Edit your profile"
                    :on-close on-close}]
     [modal/body [content]]
     [modal/footer
      [modal/button {:text "Close"
                     :on-click on-close
                     :type :secondary}]
      [modal/button {:text "Save"
                     :on-click #(dispatch [::events/update-profile])}]]]))

