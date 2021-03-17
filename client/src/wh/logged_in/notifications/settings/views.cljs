(ns wh.logged-in.notifications.settings.views
  (:require
    [re-frame.core :refer [dispatch]]
    [wh.components.common :refer [link]]
    [wh.components.forms.views :refer [select-field status-button]]
    [wh.logged-in.notifications.settings.events :as events]
    [wh.logged-in.notifications.settings.subs :as subs]
    [wh.subs :refer [<sub]]
    [wh.user.subs :as user-subs]))

(defn page []
  [:div.main
   [:div.wh-formx-page-container
    [:h1 "Notification settings"]
    [:form.wh-formx
     {:on-submit #(do (.preventDefault %)
                      (dispatch [::events/save]))}
     [:div.columns.is-variable.is-2
      [:div.column.is-7
       [:h3.notifications-explanation
        "Whenever we publish a match for your  "
        [link "preferences" :profile]
        ", you will receive notifications at "
        [:b (<sub [:user/email])] "."]
       [:p.label "Notify me when..."]
       [:fieldset
        [select-field (<sub [::subs/frequency :matching-job])
         {:options [{:id nil, :label "[Please select]"}
                    {:id "daily", :label "Daily"}
                    {:id "weekly", :label "Weekly"}
                    {:id "monthly", :label "Monthly"}
                    {:id "disabled", :label "Never"}]
          :label "A new job matches my preferences"
          :on-change [::events/edit-frequency :matching-job]}]]
       [status-button
        {:text "Save"
         :enabled? (<sub [::subs/save-enabled?])
         :waiting? (<sub [::subs/saving?])
         :status (<sub [::subs/save-status])}]]]]]])
