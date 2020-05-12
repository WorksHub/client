(ns wh.register.views
  (:require [re-frame.core :refer [dispatch]]
            [wh.components.conversation.views :refer [codi-message]]
            [wh.pages.util :refer [local-storage-supported?]]
            [wh.register.email :as email]
            [wh.register.events :as events]
            [wh.register.location :as location]
            [wh.register.name :as name]
            [wh.register.skills :as skills]
            [wh.register.subs :as subs]
            [wh.register.test :as test]
            [wh.register.verify :as verify]
            [wh.subs :refer [<sub]]))

(defn unsupported-warning []
  [:div
   [codi-message "Signup is not supported in Incognito mode."]
   [codi-message "Please enable it and try again."]])

(defn page []
  [:div.register-container
   [:div#progress-bar
    [:div.status {:style {:width (str (<sub [::subs/progress]) "%")}}]]
   [:div.columns.full-height
    [:div.column.is-half.is-offset-one-quarter.full-height
     (if (local-storage-supported?)
       (condp = (<sub [::subs/step])
         :name [name/panel]
         :skills [skills/panel]
         :email [email/panel]
         :location [location/panel]
         :verify [verify/panel]
         :test [test/panel])
       [unsupported-warning])]]])
