(ns wh.login.stackoverflow-callback.views
  (:require
    [wh.components.conversation.views :refer [error-message button]]
    [wh.components.icons :refer [icon]]
    [wh.login.stackoverflow-callback.subs :as subs]
    [wh.login.stackoverflow-callback.db :as stackoverflow-callback]
    [wh.login.views :as login-views]
    [wh.subs :refer [<sub]]))

(defn page []
  [:div.register-container
   [:div.columns.full-height
    [:div.column.is-half.is-offset-one-quarter.full-height
     (let [error (<sub [::subs/error])
           already-connected? (stackoverflow-callback/already-connected? error)
           msg (if already-connected?
                 "This Stack Overflow account is connected to another user profile."
                 "We couldn't fetch your Stack Overflow information.")]
       (when error
         [:div.conversation
          [error-message msg]
          (if already-connected?
            [login-views/back-button]
            [button
             [[icon "stackoverflow"] "Try again"] [:stackoverflow/call]])]))]]])
