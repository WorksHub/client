(ns wh.logged-in.profile.components.cover-letter
  (:require [wh.common.upload :as upload]
            [wh.logged-in.profile.components :as components]
            [wh.logged-in.profile.events :as events]
            [wh.logged-in.profile.subs :as subs]
            [wh.re-frame.subs :refer [<sub]]
            [wh.styles.profile :as styles]
            [wh.util :as util]))

(defn cover-letter-cta []
  [components/section {:class styles/cta}
   [:div
    [:h1 (util/smc styles/cta__title) "Add default cover letter"]

    [:p (util/smc styles/cta__text)
     "In your cover letter you should sell yourself and your professional
achievements in a clear and concise way. Don’t forget to
double-check the spelling and grammar."]

    [components/upload-button {:document     "cover letter"
                               :data-test    "upload-cover-letter"
                               :inverted?    true
                               :uploading?   (<sub [::subs/cover-letter-uploading?])
                               :on-change    (upload/handler
                                               :launch [::events/cover-letter-upload]
                                               :on-upload-start [::events/cover-letter-upload-start]
                                               :on-success [::events/cover-letter-upload-success]
                                               :on-failure [::events/cover-letter-upload-failure])}]]

   [:div (util/smc styles/cta__image__container--people
                   styles/cta__image__container)
    [:img {:src   "/images/profile/people_group.png"
           :class (util/mc styles/cta__image)}]]])
