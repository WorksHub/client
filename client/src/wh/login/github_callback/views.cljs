(ns wh.login.github-callback.views
  (:require
    [wh.components.conversation.views :refer [error-message button]]
    [wh.components.icons :refer [icon]]
    [wh.login.github-callback.subs :as subs]
    [wh.subs :refer [<sub]]))

(defn page []
  [:div.register-container
   [:div.columns.full-height
    [:div.column.is-half.is-offset-one-quarter.full-height
     (when (<sub [::subs/github-error?])
       [:div.conversation
        [error-message "We couldn't fetch your GitHub information."]
        [button [[icon "github"] "Try again"] [:github/call] :class "button--github"]])]]])
