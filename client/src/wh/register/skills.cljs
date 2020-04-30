(ns wh.register.skills
  (:require [re-frame.core :refer [dispatch]]
            [wh.components.bubbles.views :as bubbles]
            [wh.components.conversation.views :refer [button codi-message]]
            [wh.components.icons :refer [icon]]
            [wh.events :as whevents]
            [wh.register.conversation :refer [github-button input-with-suggestions]]
            [wh.register.events :as events]
            [wh.register.subs :as subs]
            [wh.subs :refer [<sub]]))

(defn add-skill []
  [input-with-suggestions
   :placeholder "Type a skill"
   :input-sub [::subs/new-skill]
   :set-input-ev [::events/set-new-skill]
   :dropdown-sub [::subs/suggested-skills]
   :set-dropdown-ev [::events/pick-suggested-skill]
   :on-close [::events/hide-add-skill]
   :pick-ev [::events/add-skill]])

(defn panel []
  [:div.bubble-panel.full-height
   {:on-touch-move #(.preventDefault %)}
   [:div.adder
    (when (<sub [::subs/add-skill-visible?])
      [add-skill])]
   [codi-message {:class "bubble-info",
                  :hidden? (<sub [::subs/skills-info-hidden?]),
                  :on-close [:register/hide-skills-info]}
    (if (<sub [::subs/connected-to-github?])
      [:span "We’ve done the hard work for you by pre-selecting your skills based on your GitHub profile. Large and green is selected, red and small is not. Feel free to " [:span.highlight "customise your picks"]]
      [:span [:span.highlight "Select your preferred skills. "]
             "Large and green is selected, red and small is not. "
             (when-not (or (<sub [::subs/stackoverflow-signup?])
                           (<sub [::subs/twitter-signup?]))
               [:<>
                [:br.is-hidden-desktop]
                [:span.highlight "Connect your GitHub "]
                "to save some time. We will pre-select skills for you. "])])]
   [bubbles/bubbles (<sub [::subs/skills])
    :on-size-change #(dispatch [::events/select-skill %1 %2])]
   (when-not (or (<sub [::subs/connected-to-github?])
                 (<sub [::subs/stackoverflow-signup?])
                 (<sub [::subs/twitter-signup?]))
     [github-button])
   [:div.bubble-panel__bottom
    [button "I'm all done" [:register/advance]
     :disabled (<sub [::subs/cannot-proceed-from-skills?])]
    [button "Something's missing…"
     [::events/show-add-skill]
     :disabled (<sub [::subs/add-skill-visible?])]]])
