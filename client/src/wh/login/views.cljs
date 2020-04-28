(ns wh.login.views
  (:require [re-frame.core :refer [dispatch dispatch-sync]]
            [wh.common.re-frame-helpers :refer [dispatch-on-enter]]
            [wh.components.button-auth :as button-auth]
            [wh.components.common :refer [link]]
            [wh.components.icons :refer [icon]]
            [wh.login.events :as events]
            [wh.login.subs :as subs]
            [wh.subs :refer [<sub]]))

(defmulti status-message identity) ;; TODO: move to a subscription

(defmethod status-message :not-posted []
  nil)

(defmethod status-message :email-not-sent []
  [:p.help.help--medium.is-danger "Email was not sent (use whitelisted emails in dev/staging)."])

(defmethod status-message :invalid-arguments []
  [:p.help.help--medium.is-danger "Please ensure you have supplied a valid email address."])

(defmethod status-message :no-user-found-with-email [_ email]
  [:p.help.help--medium.is-danger
    "There is no account for email " email [:br]
    "If you are a company user, please contact your company manager or talk to us now via live chat."])

(defmethod status-message :unknown-error []
  [:p.help.help--medium.is-danger "There was an error processing the request. Please try again later."])

(defmethod status-message :email-unsuccessful []
  [:p.help.help--medium.is-danger "There was an problem with sending a login email. Please try again later."])

(defn magic-form []
  (let [email (<sub [::subs/magic-email])]
    [:div.container.login-buttons
     [:div.wh-form.wh-form--left.wh-form--compact.wh-form__layout
      [:div.field
       [:label.label "Email"]]
      [:div.control
       [:input.input
        {:type        "text"
         :value       email
         :placeholder "Email address"
         :on-key-up   (dispatch-on-enter [::events/send-magic-email])
         :on-change   #(dispatch-sync [::events/set-magic-email (-> % .-target .-value)])}]
       [status-message (<sub [::subs/magic-status]) email]]
      [:button.button.button--medium
       {:on-click #(dispatch [::events/send-magic-email])
        :disabled (<sub [::subs/invalid-magic-email?])}
       "Send Login Link"]]]))

(defn login-buttons
  []
  [:div.container.login-buttons
   [:div.container
    [button-auth/button :github {:text "Login with Github" :id "auth-github"}]]
   [:div.container
    [button-auth/button :stackoverflow {:text "Login with Stack Overflow"}]]
   [:div.container
    [button-auth/button :email-signin {:text "Login with Email" :id "auth-email-signin"}]]
   [:div.container
    [link "Create Account" :get-started :class "a--underlined"]]
   [:img.sparkle {:src "/images/sparkle.svg"
                  :alt "Background sparkle"}]])

(defn page []
  [:div.main
    (if (<sub [::subs/magic-success?])
      [:div
        [:h1 "Login"]
        [:h2 "Your Magic Link is on its way. Check your email inbox."]]
      [:div
        [:h1 "Login"]
        [:h2 "to continue to " (<sub [::subs/continue-to])]
        (if (<sub [::subs/show-magic-form?])
          [magic-form]
          [login-buttons])])])
