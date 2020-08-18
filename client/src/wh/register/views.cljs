(ns wh.register.views
  (:require [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
            [wh.components.signin-buttons :as signin-button]
            [wh.routes :as routes]
            [wh.styles.register :as styles]
            [wh.util :as util]
            [wh.subs :refer [<sub]]
            [wh.register.subs :as subs]
            [wh.register.events :as events]))

(defn title []
  [:div (util/smc styles/title) "Sign Up"])

(def stackoverflow-button-content
  [:div [:div (util/smc styles/display-not-mobile) "Continue with Stack Overflow"]
   [:div (util/smc styles/display-mobile) "Stack Overflow"]])

(defn auth-buttons []
  [:div (util/smc styles/auth-buttons)
   [signin-button/github {:text "Continue with Github" :type :signup}]
   [signin-button/twitter {:text "Continue with Twitter" :type :signup}]
   [signin-button/stack-overflow {:text stackoverflow-button-content :type :signup}]])

(defn error-message [text]
  [:span (util/smc styles/error) text])

(defn field-name []
  (let [error (<sub [::subs/error-name])]
    [:label (util/smc styles/label)
     "Name"
     [:input
      {:type       :text
       :value      (<sub [::subs/name])
       :on-change  #(dispatch-sync [::events/set-name (-> % .-target .-value)])
       :aria-label "Name"
       :data-test "name"
       :placeholder "Name"
       :class (util/mc styles/input [error styles/input--error])}]
     [error-message error]]))

(defn field-email []
  (let [error (<sub [::subs/error-email])]
    [:label (util/smc styles/label styles/label--email)
     "Email"
     [:input {:type :email
              :placeholder "Email"
              :aria-label "Email"
              :data-test "email"
              :class (util/mc styles/input [error styles/input--error])
              :value      (<sub [::subs/email])
              :on-change  #(dispatch-sync [::events/set-email (-> % .-target .-value)])}]
     [error-message error]]))

(defn submit-button []
  (let [submitting? (<sub [::subs/submitting?])]
    [:button {:class styles/button
              :on-click #(dispatch [::events/create-user])
              :type "button"
              :data-test "signup"
              :disabled submitting?}
     (if submitting? "Continue..." "Continue")]))


(defn error-unhandled []
  (when-let [error-unhandled (<sub [::subs/error-unhandled])]
    [error-message error-unhandled]))

(defn form []
  [:form
   [field-name]
   [field-email]
   [error-unhandled]
   [submit-button]])

(defn link [{:keys [text href]}]
  [:a {:class styles/link :href href} text])

(defn message-user-agreement []
  [:p (util/smc styles/paragraph)
   "By clicking continue, you agree to our" [link {:text " Terms of Service"
                                                   :href (routes/path :terms-of-service)}]
   " and" [link {:text " Privacy Policy"
                 :href (routes/path :privacy-policy)}]])

(defn message-existing-account []
  [:p (util/smc styles/paragraph)
   "Already have an account?"
   [link {:text " Login"
          :href (routes/path :login :params {:step :root})}]])

(defn stackoverflow-message []
  [:div (util/smc styles/paragraph styles/stackoverflow)
   "Stack Overflow doesn't provide your email information, please fill it in. Thank you"])

(defn card-signup []
  (let [stackoverflow-signup? (<sub [::subs/stackoverflow-signup?])]
    [:div {:class styles/container}
     [:div {:class styles/card
            :data-test "form-signup"}
      [title]
      (when stackoverflow-signup? [stackoverflow-message])
      (when-not stackoverflow-signup? [auth-buttons])
      [form]
      [message-user-agreement]
      [:hr (util/smc styles/separator)]
      [message-existing-account]]]))
