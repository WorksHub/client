(ns wh.components.auth
  (:require [wh.components.icons :refer [icon]]
            [wh.components.signin-buttons :as signin-button]
            [wh.routes :as routes]
            [wh.styles.register :as styles]
            [wh.util :as util]))

(defn title [text]
  [:div (util/smc styles/title) text])

(defn header [& children]
  [:div (util/smc styles/header)
   children])

(defn link [{:keys [text href class target]}]
  [:a {:class (util/mc styles/link [class class]) :href href :target target} text])

(defn paragraph [& children]
  (into [:p (util/smc styles/paragraph)] children))

(defn paragraph-bold [& children]
  (into [:p (util/smc styles/paragraph styles/paragraph--bold)] children))

(defn separator []
  [:hr (util/smc styles/separator)])

(defn button [{:keys [submitting? text on-click data-test]
               :or   {text "Continue"}}]
  [:button {:class     styles/button
            :on-click  on-click
            :data-test data-test
            :disabled  submitting?}
   (cond-> text
           submitting? (str "..."))])

(defn social-buttons []
  (let [stackoverflow-button-content
        [:div [:div (util/smc styles/display-not-mobile) "Continue with Stack Overflow"]
         [:div (util/smc styles/display-mobile) "Stack Overflow"]]]
    [:div (util/smc styles/auth-buttons)
     [signin-button/github {:text      "Continue with GitHub"
                            :type      :auth
                            :data-test "auth-github"}]
     [signin-button/twitter {:text      "Continue with Twitter"
                             :type      :auth
                             :data-test "auth-twitter"}]
     [signin-button/stack-overflow {:text      stackoverflow-button-content
                                    :type      :auth
                                    :data-test "auth-stackoverflow"}]
     [signin-button/employers]]))

(defn page [& children]
  (into [:div {:class styles/container}] children))

(defn form [opts & children]
  (into [:form (merge {:class styles/form} opts)] children))

(defn card [{:keys [type data-test] :as args} & children]
  (into [:div {:data-test data-test
               :class (util/mc styles/card
                               [(= type :invalid-link) styles/card--invalid-link]
                               [(= type :check-email) styles/card--check-email])}]
        children))

(defn stackoverflow-message []
  [:div (util/smc styles/paragraph styles/info)
   "Stack Overflow doesn't provide your email information, please fill it in. Thank you"])

(defn twitter-message []
  [:div (util/smc styles/paragraph styles/info)
   "Your Twitter profile doesn't have email information, please fill it in. Thank you"])

(defn error-message [message]
  (when message [:span (util/smc styles/error) message]))

(defn field [{:keys [error label] :as opts}]
  [:label (util/smc styles/label)
   label
   [:input (merge opts {:class (util/mc styles/input [error styles/input--error])})]
   [error-message error]])
