(ns wh.register.views
  (:require [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
            [wh.components.auth :as auth]
            [wh.routes :as routes]
            [wh.styles.register :as styles]
            [wh.util :as util]
            [wh.subs :refer [<sub]]
            [wh.register.subs :as subs]
            [wh.register.events :as events]))

(defn field-name []
  [auth/field {:type       :text
               :value      (<sub [::subs/name])
               :on-change  #(dispatch-sync [::events/set-name (-> % .-target .-value)])
               :aria-label "Name"
               :data-test "name"
               :autocomplete "name"
               :placeholder "Name"
               :label "Name"
               :error (<sub [::subs/error-name])}])

(defn field-email []
  [auth/field {:type :email
               :placeholder "Email"
               :aria-label "Email"
               :data-test "email"
               :autocomplete "email"
               :value      (<sub [::subs/email])
               :on-change  #(dispatch-sync [::events/set-email (-> % .-target .-value)])
               :label "Email"
               :error (<sub [::subs/error-email])}])

(defn form-signup []
  [auth/form
   {:on-submit #(do (.preventDefault %)
                    (dispatch [::events/create-user]))}
   [:div (util/smc styles/fields)
    [field-name]
    [field-email]]
   [auth/error-message (<sub [::subs/error-unhandled])]
   [auth/button {:submitting? (<sub [::subs/submitting?])
                 :data-test "signup"}]])

(defn message-user-agreement []
  [auth/paragraph
   "By clicking continue, you agree to our" [auth/link {:text " Terms of Service"
                                                        :href (routes/path :terms-of-service)}]
   " and" [auth/link {:text " Privacy Policy"
                      :href (routes/path :privacy-policy)}]])

(defn message-existing-account []
  [auth/paragraph
   "Already have an account?"
   [auth/link {:text " Login"
               :href (routes/path :login :params {:step :root})}]])

(defn card-signup []
  [auth/page
   [auth/card {:type :default
               :data-test "form-signup"}
    [auth/title "Sign Up"]
    (when (<sub [::subs/stackoverflow-signup?]) [auth/stackoverflow-message])
    (when-not (<sub [::subs/stackoverflow-signup?]) [auth/social-buttons])
    [form-signup]
    [message-user-agreement]
    [auth/separator]
    [message-existing-account]]])
