(ns wh.login.twitter-callback.views
  (:require
    [wh.components.conversation.views :refer [error-message button]]
    [wh.components.icons :refer [icon]]
    [wh.login.twitter-callback.subs :as subs]
    [wh.login.twitter-callback.db :as twitter-callback]
    [wh.subs :refer [<sub]]))

(defn page []
  [:div.register-container
   [:div.columns.full-height
    [:div.column.is-half.is-offset-one-quarter.full-height
     (let [error (<sub [::subs/error])
           already-connected? (twitter-callback/already-connected? error)
           msg (if already-connected?
                 "This Twitter account is connected to another user profile."
                 "We couldn't fetch your twitter information.")]
       (when error
         [:div.conversation
          [error-message msg]
          (when-not already-connected?
            [button
             [[icon "twitter"] "Try again"] [:twitter/call]])]))]]])
