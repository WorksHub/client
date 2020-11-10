(ns wh.logged-in.profile.components.cv
  (:require [wh.common.upload :as upload]
            [wh.logged-in.profile.components :as components]
            [wh.logged-in.profile.events :as events]
            [wh.logged-in.profile.subs :as subs]
            [wh.re-frame.events :refer [dispatch]]
            [wh.re-frame.subs :refer [<sub]]
            [wh.styles.profile :as styles]
            [wh.util :as util]))

(defn cv-cta []
  [components/section
   [:div (util/smc styles/cta__container)
    [:div
     [:h1 (util/smc styles/cta__title) "Get yourself out there"]

     [:p (util/smc styles/cta__text)
      "Submit your resume. It will be given to companies when you apply for a role.
You can update it anytime from your profile."]

     [:div (util/smc styles/cta__section)
      [components/upload-button {:document   "resume"
                                 :uploading? (<sub [::subs/cv-uploading?])
                                 :inverted?  true
                                 :on-change  (upload/handler
                                               :launch [::events/cv-upload]
                                               :on-upload-start [::events/cv-upload-start]
                                               :on-success [::events/cv-upload-success]
                                               :on-failure [::events/cv-upload-failure])}]

      [:p (util/smc styles/cta__section__seperator) "OR"]

      (let [cv-link-val (<sub [::subs/cv-link-editable])
            save-link!  #(dispatch [::events/save-cv-info
                                    {:type    :update-cv-link
                                     :cv-link cv-link-val}])]
        [:div (util/smc styles/cta__text-container)
         [components/text-field cv-link-val
          {:on-change   [::events/edit-cv-link-editable]
           :on-enter    save-link!
           :placeholder "Enter link to resume"
           :class       styles/cta__text-field}]

         [components/small-button
          {:on-click  save-link!
           :inverted? true}
          "Save"]])]]

    [:div (util/smc styles/cta__image__container--document
                    styles/cta__image__container)
     [:img {:src   "/images/profile/document2.png"
            :class (util/mc styles/cta__image)}]]]])
