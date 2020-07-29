(ns wh.login.github-callback.views
  (:require
    [wh.components.conversation.views :refer [error-message button]]
    [wh.components.icons :refer [icon]]
    [wh.login.github-callback.db :as github-callback]
    [wh.login.github-callback.subs :as subs]
    [wh.login.views :as login-views]
    [wh.subs :refer [<sub]]))

(defn page []
  [:div.register-container
   [:div.columns.full-height
    [:div.column.is-half.is-offset-one-quarter.full-height
     (let [error              (<sub [::subs/error])
           already-connected? (github-callback/already-connected? error)
           msg                (if already-connected?
                                "This GitHub account is connected to another user profile."
                                "We couldn't fetch your GitHub information.")]
       (when error
         [:div.conversation
          [error-message msg]
          (if already-connected?
            [login-views/back-button]
            [button
             [[icon "github"] "Try again"] [:github/call]])]))]]])
