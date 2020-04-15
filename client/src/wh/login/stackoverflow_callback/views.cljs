(ns wh.login.stackoverflow-callback.views
  (:require
    [wh.components.conversation.views :refer [error-message button]]
    [wh.components.icons :refer [icon]]
    [wh.login.stackoverflow-callback.subs :as subs]
    [wh.subs :refer [<sub]]))

(defn page []
  [:div.register-container
   [:div.columns.full-height
    [:div.column.is-half.is-offset-one-quarter.full-height
     (when (<sub [::subs/error?])
       [:div.conversation
        [error-message "We couldn't fetch your StackOverflow information."]
        [button [[icon "stackoverflow"] "Try again"] [:stackoverflow/call]]])]]])
