(ns wh.login.views
  (:require [re-frame.core :refer [dispatch dispatch-sync]]
            [wh.components.auth :as auth]
            [wh.components.common :refer [link]]
            [wh.components.conversation.views :refer [button]]
            [wh.components.icons :refer [icon]]
            [wh.login.events :as events]
            [wh.login.subs :as subs]
            [wh.routes :as routes]
            [wh.styles.register :as styles]
            [wh.subs :refer [<sub]]
            [wh.util :as util]))

(defn back-button
  []
  [button "Back to Profile" [::events/go-profile]])

(defn field-email []
  [auth/field {:type        :email
               :placeholder "Email"
               :aria-label  "Email"
               :data-test   "email"
               :label       "Email"
               :value       (<sub [::subs/email])
               :on-change   #(dispatch-sync [::events/set-email (-> % .-target .-value)])}])

(defn form-signin []
  [auth/form
   {:on-submit #(do (.preventDefault %)
                    (dispatch [::events/send-login-link]))}
   [field-email]
   [auth/error-message (<sub [::subs/error-message])]
   [auth/button {:data-test   "signin"
                 :submitting? (<sub [::subs/submitting?])}]])

(defn check-you-email [email]
  [auth/card {:type :check-email
              :data-test "check-email"}
   [auth/title "Check email"]
   [:div (util/smc styles/about-confirmation)
    [auth/paragraph "We sent a confirmation link to"]
    [auth/paragraph-bold email]
    [auth/paragraph "Check your email and click on the confirmation link to continue"]]
   [:div (util/smc styles/confirmation-image)]
   [auth/paragraph
    "Didn't receive a link?"
    [auth/link {:text " Try again"
                :href (routes/path :login :params {:step :email})}]]])

(defn message-non-existing-account []
  [auth/paragraph
   "Don't have an account?"
   [auth/link {:text " Sign Up"
               :href (routes/path :get-started)}]])

(defn page []
  [auth/page
   [auth/card {:type :default
               :data-test "form-signin"}
    (when (<sub [::subs/email-sent?])
      [check-you-email (<sub [::subs/email])])
    [auth/title "Login"]
    [auth/social-buttons]
    [form-signin]
    [auth/separator]
    [message-non-existing-account]]])
