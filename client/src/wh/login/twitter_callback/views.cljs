(ns wh.login.twitter-callback.views
  (:require
    [wh.components.conversation.views :refer [error-message button]]
    [wh.components.icons :refer [icon]]
    [wh.login.twitter-callback.subs :as subs]
    [wh.subs :refer [<sub]]))

(defn page []
  [:div.register-container
   [:div.columns.full-height
    [:div.column.is-half.is-offset-one-quarter.full-height
     (when (<sub [::subs/error?])
       [:div.conversation
        [error-message "We couldn't fetch your twitter information."]
        [button
         [[icon "twitter"] "Try again"] [:twitter/call]]])]]])
