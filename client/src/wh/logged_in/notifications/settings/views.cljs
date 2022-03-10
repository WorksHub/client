(ns wh.logged-in.notifications.settings.views
  (:require
    [re-frame.core :refer [dispatch]]
    [wh.components.common :refer [link]]
    [wh.components.forms.views :refer [select-field status-button]]
    [wh.logged-in.notifications.settings.events :as events]
    [wh.logged-in.notifications.settings.subs :as subs]
    [wh.notification-settings.styles :as styles]
    [wh.subs :refer [<sub]]
    [wh.user.subs]
    [wh.util :as util]))

(defn page []
  [:div.main
   [:div.wh-formx-page-container
    [:h1 "Notification settings"]
    [:form
     {:class     (util/mc "wh-formx" styles/wh-formx)
      :on-submit #(do (.preventDefault %)
                      (dispatch [::events/save]))}
     [:div.columns.is-variable.is-2
      [:div.column.is-7
       [:p {:class styles/text}
        "Whenever we publish a match for your "
        [link "preferences" :profile]
        ", you will receive notifications at "
        [:b (<sub [:user/email])] "."]
       [:fieldset
        [select-field (<sub [::subs/frequency :matching-job])
         {:options     [{:id nil, :label "[Please select]"}
                        {:id "disabled", :label "Never"}
                        {:id "daily", :label "Daily"}
                        {:id "weekly", :label "Weekly"}
                        {:id "monthly", :label "Monthly"}]
          :class       styles/field
          :label       "Notify me when a new job matches my preferences"
          :label-class styles/label
          :on-change   [::events/edit-frequency :matching-job]}]]
       [status-button
        {:text     "Save"
         :enabled? (<sub [::subs/save-enabled?])
         :waiting? (<sub [::subs/saving?])
         :status   (<sub [::subs/save-status])}]]]]]])
