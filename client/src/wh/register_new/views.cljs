(ns wh.register-new.views
  (:require [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
            [wh.components.signin-buttons :as signin-button]
            [wh.routes :as routes]
            [wh.styles.register :as styles]
            [wh.util :as util]))

(defn title []
  [:div (util/smc styles/title) "Sign Up"])

(defn button [{:keys [text]}]
  [:a (util/smc styles/button) text])

(def stackoverflow-button-content
  [:div [:div (util/smc styles/display-not-mobile) "Continue with Stack Overflow"]
   [:div (util/smc styles/display-mobile) "Stack Overflow"]])

(defn auth-buttons []
  [:div (util/smc styles/auth-buttons)
   [signin-button/github {:text "Continue with Github" :type :signup}]
   [signin-button/twitter {:text "Continue with Twitter" :type :signup}]
   [signin-button/stack-overflow {:text stackoverflow-button-content :type :signup}]])

(defn input []
  [:label (util/smc styles/label)
   "Email"
   [:input {:type "email"
            :placeholder "Email"
            :class styles/input}]])

(defn form []
  [:form
   [:label (util/smc styles/label)
    "Name"
    [:input {:type "text"
             :placeholder "Name"
             :class styles/input}]]
   [:label (util/smc styles/label styles/label--email)
    "Email"
    [:input {:type "email"
             :placeholder "Email"
             :class styles/input}]]
   [button {:text "Continue"}]])

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

(defn card-signup []
  [:div {:class styles/container}
   [:div {:class styles/card}
    [title]
    [auth-buttons]
    [form]
    [message-user-agreement]
    [:hr (util/smc styles/separator)]
    [message-existing-account]]])
