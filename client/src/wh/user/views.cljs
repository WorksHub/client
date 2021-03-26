(ns wh.user.views
  (:require ["react-dom" :as react-dom]
            [re-frame.core :refer [dispatch]]
            [reagent.core :as r]
            [wh.subs :refer [<sub]]
            [wh.user.subs :as subs]))

(defn consent-popup* []
  [:div.consent-popup
   [:p "Functional Works Limited (and our sub brands) remain fully committed to the protection of your privacy at all times.
    We follow the principles of the General Data Protection Regulation of May 2018. We have a designated Data Protection Officer,
     and accountability and privacy are principles that are designed into both our software and policies.
      The information contained in our "
    [:a.a--underlined {:href   "/privacy-policy"
                       :target "_blank"
                       :rel    "noopener"}
     "privacy policy"] " has been published to inform you of the way in which any Personal Data (as defined)
       you provide us with or we collect from you will be used. Please read this information carefully in order to fully understand how we treat such Personal Data."]
   [:button.button {:on-click #(dispatch [:user/save-consent])
                    :class    (when (<sub [::subs/saving-consent?]) "button--light button--loading")
                    :disabled (<sub [::subs/saving-consent?])} "Agree"]
   (when (<sub [::subs/save-consent-error?])
     [:div.consent-error.conversation-element--error
      "There was an error saving your consent, please try again."])])

(defn consent-popup []
  (let [el    (.. js/document (getElementById "overlays"))
        show? (<sub [::subs/show-consent-popup?])]
    (when show?
      (react-dom/createPortal (r/as-element [consent-popup*]) el))))
