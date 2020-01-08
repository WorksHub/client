(ns wh.register.email
  (:require
    [re-frame.core :refer [dispatch dispatch-sync]]
    [wh.components.conversation.views :refer [codi-message error-message button]]
    [wh.components.forms.views :refer [target-checked]]
    [wh.components.icons :refer [icon]]
    [wh.re-frame.subs :refer [<sub]]
    [wh.register.events :as events]
    [wh.register.subs :as subs]
    [wh.user.subs :as user-subs]))

(defn email-box []
  [:div.animatable
   [:form.conversation-element.user
    {:on-submit #(do (.preventDefault %)
                     (dispatch [::events/upsert-user]))}
    [:input {:type       :email
             :auto-focus true
             :value      (<sub [::subs/email])
             :on-change  #(dispatch-sync [::events/set-email (-> % .-target .-value)])
             :aria-label "Email input"}]]])

(defn consent-checkbox []
  [:div.animatable
   [:div.checkbox.consent-checkbox.conversation-element.user
    [:div
     [:input
      {:type      "checkbox"
       :id        "consent"
       :checked   (<sub [::subs/consented?])
       :on-change #(dispatch-sync [::events/set-consent (target-checked %)])}]
     [:label#consent-label.is-flex {:for "consent"}
      [:div {:class "checkbox__box"}]
      [:span
       "By submitting this form, you agree to opt-in to the "
       [:a.a--underlined {:href   "/privacy-policy"
                          :target "_blank"
                          :rel    "noopener"}
        "privacy policy"] " of this website and the processing of your data."]]]
    [:div.newsletter-section

     [:input
      {:type      "checkbox"
       :id        "newsletter"
       :checked   (<sub [::subs/subscribed?])
       :on-change #(dispatch-sync [::events/set-subscribed (target-checked %)])}]
     [:label#newsletter-label {:for "newsletter"}
      [:div {:class "checkbox__box"}]
      [:span "Don't get FOMO \uD83D\uDE40 and subscribe to our newsletter"]]]]])

(defn panel []
  [:div
   [codi-message "Hi, I am Codi. Welcome to " (<sub [:wh/platform-name]) "!"]
   [codi-message (if (<sub [:user/email])
                   "Is this the best email for your account?"
                   "Please enter your email.")]
   [email-box]
   [consent-checkbox]
   [button "Next"
    [:register/proceed-from-email]
    :class (when (<sub [::subs/loading?]) "button--loading")
    :disabled (<sub [::subs/loading?])]
   (when-let [error (<sub [::subs/upsert-user-errors])]
     [error-message error])])
