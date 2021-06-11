(ns wh.profile.update-private.views
  (:require [re-frame.core :refer [dispatch]]
            [wh.components.forms.views :as forms]
            [wh.profile.update-private.events :as events]
            [wh.profile.update-private.subs :as subs]
            [wh.styles.profile :as styles]
            [wh.subs :refer [<sub]]
            [wh.util :as util]))

(defn current-location []
  [forms/text-field-with-label (<sub [::subs/current-location-label])
   {:label                "Current location"
    :data-test            :current-location
    :suggestions          (<sub [::subs/current-location-suggestions])
    :on-change            [::events/edit-current-location]
    :on-select-suggestion [::events/select-current-location-suggestion]
    :placeholder          "Type to search location..."}])

(defn preferred-location [value {:keys [i label]}]
  [forms/text-field value
   {:new?                 true
    :label                label
    :data-test            :preferred-location
    :suggestions          (<sub [::subs/location-suggestions i])
    :on-change            [::events/edit-preferred-location i]
    :on-select-suggestion [::events/select-suggestion i]
    :on-remove            [::events/remove-preferred-location i]
    :placeholder          "Type to search location..."}])

(defn preferred-locations []
  [:div
   [forms/label-text {:label "Preferred locations"}]
   [forms/multi-edit
    (<sub [::subs/preferred-location-labels])
    {:label         "Preferred locations"
     :component     preferred-location
     :class-wrapper styles/private-update__preferred-locations}]])

(defn email []
  [:div
   [forms/text-input-with-label
    (<sub [::subs/email])
    {:label       "Email"
     :required?   true
     :placeholder "Email"
     :on-change   [::events/edit-email]
     :data-test :email}]
   [forms/error-component (<sub [::subs/field-error :email])]])

(defn phone []
  [forms/text-input-with-label
   (<sub [::subs/phone])
   {:label       "Phone"
    :placeholder "Phone"
    :on-change   [::events/edit-phone]
    :data-test :phone}])

(defn role-type-and-visa-status []
  [:div {:class styles/private-update__checkboxes-wrapper}
   [:label
    [forms/label-text {:label "Visa Status"}]
    [forms/multiple-checkboxes
     (<sub [::subs/visa-status])
     {:options       (<sub [::subs/visa-status-options])
      :on-change     [::events/toggle-visa-status]
      :class-wrapper styles/private-update__checkboxes}]]
   [:label
    [forms/label-text {:label "Role Types"}]
    [forms/multiple-checkboxes
     (<sub [::subs/role-types])
     {:options       (<sub [::subs/role-type-options])
      :on-change     [::events/toggle-role-type]
      :class-wrapper styles/private-update__checkboxes}]]])

(defn salary []
  [:div
   [:div {:class styles/private-update__salary}
    [forms/text-input-with-label
     (<sub [::subs/salary-min])
     {:label       "Minimum Compensation"
      :placeholder "Salary + other compensation etc"
      :type        "number"
      :on-change   [::events/edit-salary-min]
      :data-test   :salary}]
    [forms/select-field
     (<sub [::subs/salary-currency])
     {:new?      true
      :options   (<sub [::subs/currency-options])
      :label     "Currency"
      :on-change [::events/edit-salary-currency]
      :data-test :currency}]
    [forms/select-field
     (<sub [::subs/salary-time-period])
     {:new?      true
      :options   (<sub [::subs/time-period-options])
      :label     "Time period"
      :on-change [::events/edit-salary-time-period]
      :data-test :time-period}]]
   [forms/error-component (<sub [::subs/field-error :salary])]])

(defn job-seeking-status []
  [forms/select-field-with-label
   (<sub [::subs/job-seeking-status])
   {:options   (<sub [::subs/job-seeking-statuses])
    :label     "Job seeking status"
    :on-change [::events/edit-job-seeking-status]
    :data-test :job-seeking-status}])

(defn visa-status-other []
  (when (<sub [::subs/show-visa-status-other?])
    [forms/text-input-with-label
     (<sub [::subs/visa-status-other])
     {:label       "Other visa status"
      :placeholder "Other visa status"
      :on-change   [::events/edit-visa-status-other]}]))

(defn remote []
  [:label
   [forms/label-text {:label "Remote work"}]
   [forms/labelled-checkbox
    (<sub [::subs/prefer-remote?])
    {:label     "Prefer remote work"
     :on-change [::events/toggle-prefer-remote]}]])

(defn buttons []
  [:div {:class styles/private-update__buttons}
   [:button
    {:on-click #(dispatch [::events/close-form])
     :class    (util/mc styles/button styles/button--small styles/button--inverted)}
    "Cancel"]
   [:button
    {:on-click #(dispatch [::events/update-profile])
     :class    (util/mc styles/button styles/button--small)
     :data-test :save}
    "Save"]])

(defn profile-edit-inline []
  (when (<sub [::subs/editing-profile?])
    [:div {:class styles/private-update__wrapper--outer
           :data-test :edit-private-info}
     [:div {:class styles/private-update__wrapper}
      [email]
      [phone]
      [job-seeking-status]
      [salary]
      [role-type-and-visa-status]
      [visa-status-other]
      [current-location]
      [preferred-locations]
      [remote]]
     [buttons]]))
