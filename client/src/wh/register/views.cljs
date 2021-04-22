(ns wh.register.views
  (:require [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
            [wh.components.auth :as auth]
            [wh.components.icons :refer [icon]]
            [wh.register.events :as events]
            [wh.register.subs :as subs]
            [wh.routes :as routes]
            [wh.styles.register :as styles]
            [wh.subs :refer [<sub]]
            [wh.util :as util]))

(defn field-name []
  (let [on-change  #(dispatch-sync [::events/set-name (-> % .-target .-value)])]
    (fn []
      [auth/field {:type       :text
                   :on-change on-change
                   :aria-label "Name"
                   :data-test "name"
                   :autoComplete "name"
                   :placeholder "Name"
                   :label "Name"
                   :value (<sub [::subs/name])
                   :error (<sub [::subs/error-name])}])))

(defn field-email []
  (let [on-change #(dispatch-sync [::events/set-email (-> % .-target .-value)])]
    (fn []
      [auth/field {:type :email
                   :placeholder "Email"
                   :aria-label "Email"
                   :data-test "email"
                   :autoComplete "email"
                   :on-change on-change
                   :value  (<sub [::subs/email])
                   :label "Email"
                   :error (<sub [::subs/error-email])}])))

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
   "By clicking continue, you agree to our"
   [auth/link {:text " Terms of Service"
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
   [auth/card {:type      :default
               :data-test "form-signup"}
    [auth/header
     ^{:key :title}
     [auth/title "Sign Up"]
     ^{:key :link}
     [auth/link {:text [:span
                        "Post a Job"
                        [icon "arrow-right" :class styles/arrow]]
                 :class styles/post-job
                 :href (routes/path :register-company)}]]
    (when (<sub [::subs/stackoverflow-signup?]) [auth/stackoverflow-message])
    (when (<sub [::subs/twitter-signup?]) [auth/twitter-message])
    (when-not (or (<sub [::subs/stackoverflow-signup?])
                  (<sub [::subs/twitter-signup?]))
      [auth/social-buttons])
    [form-signup]
    [message-user-agreement]
    [auth/separator]
    [message-existing-account]]])
