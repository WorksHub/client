(ns wh.notification-settings.views
  (:require [wh.common.subs]
            [wh.components.forms :as forms]
            [wh.components.icons :refer [icon]]
            [wh.notification-settings.styles :as styles]
            [wh.notification-settings.subs :as subs]
            [wh.re-frame.subs :refer [<sub]]
            [wh.routes :as routes]
            [wh.util :as util]))

(defn submittable-radio
  "This is a hack used to pass a checked radio value via an ordinary HTML <form> submit."
  [name value options]
  [:<>
   [:input.visually-hidden
    {:id   (str name "-id")
     :name name}]
   [forms/radio-field value
    {:options     options
     :class       (util/mc styles/field styles/submittable-radio)
     :label       "Notify me when a new job matches my preferences"
     :label-class styles/label
     :on-change   (str "document.getElementById('" name "-id').value = this.dataset.id;")}]])

(defn page []
  (let [matching-job-freq (<sub [::subs/frequency :matching-job])
        frequency-options (<sub [::subs/frequency-options])
        user-token        (<sub [::subs/user-token])
        updated?          (<sub [::subs/updated?])
        wrong-arguments?  (<sub [::subs/wrong-arguments?])]
    ;; NB: Copycats the CLJS-specific `wh.logged-in.notifications.settings.views`.
    [:div.main
     [:div.wh-formx-page-container
      [:h1 "Notification settings"]
      [:form
       {:method "post"
        :action (routes/path :notification-settings-form)
        :class  (util/mc "wh-formx" styles/wh-formx)}
       [:div.columns.is-variable.is-2
        [:div.column.is-7
         [:p {:class styles/text}
          "Whenever we publish a match for your preferences, you will receive notifications at your email."]
         [:fieldset
          [submittable-radio "matching-job-frequency" matching-job-freq frequency-options]
          [:input.visually-hidden
           {:name  "user-token"
            :value user-token}]]
         [:input.button.button--small
          {:id    "submit"
           :type  "submit"
           :value "Submit"}]
         (when updated?
           [:<>
            [:br]
            [:span.status.status--good "Updated!"]])
         (when wrong-arguments?
           [:<>
            [:br]
            [:span.status.status--bad "Wrong arguments!"]])]]]]]))
