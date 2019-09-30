(ns wh.logged-in.profile.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch dispatch-sync]]
            [reagent.core :as reagent]
            [wh.common.specs.primitives :as p]
            [wh.common.text :refer [pluralize]]
            [wh.common.upload :as upload]
            [wh.common.user :as common-user]
            [wh.components.common :refer [link]]
            [wh.components.error.views :refer [error-box]]
            [wh.components.forms.views :refer [field-container
                                               labelled-checkbox
                                               multi-edit multiple-buttons
                                               select-field select-input
                                               text-field text-input]]
            [wh.components.icons :refer [icon]]
            [wh.logged-in.profile.events :as events]
            [wh.logged-in.profile.subs :as subs]
            [wh.routes :as routes]
            [wh.subs :refer [<sub]]
            [wh.user.subs :as user-subs]))

;; Profile header – view

(defn avatar [{:keys [image-url]}]
  [:div.avatar
   [:img {:src (or image-url (common-user/random-avatar-url))
          :alt "User's avatar"}]])

(defn name-view [{:keys [name]}]
  [:div.name name])

(defn skills [opts]
  [:div.skills
   (into [:ul.tags]
         (for [{:keys [name rating]} (:skills opts)]
           [:li name (when rating [icon (str "rating-" rating)])]))])

(defn linkbox [{:keys [other-urls]}]
  (into
   [:div.linkbox]
   (for [{:keys [url type display]} other-urls]
     [:span [icon (name type)] [:a {:href url
                                    :target "_blank"
                                    :rel "noopener"} display]])))

(defn summary [opts]
  (let [summary (:summary opts)]
    (when-not (str/blank? summary)
      [:div.summary summary])))

(defn owner? [user-type] (= user-type :owner))
(defn admin? [user-type] (= user-type :admin))
(defn owner-or-admin? [user-type] (contains? #{:owner :admin} user-type))

(defn contributions [user-type opts]
  (let [contributions (:contributions opts)]
    (if (seq contributions)
      [:div.contributions
       [:h2 "Contributions"]
       (into [:ul]
             (for [{:keys [id title upvote-count]} contributions]
               [:li
                [link title :blog :id id :class "link"]
                " | "
                upvote-count " " (pluralize upvote-count "boost")]))]
      (when (owner? user-type)
        [:div.empty-contributions
         [link "Contribute" :contribute :class "button"]]))))

(defn edit-link
  ([profile-page candidate-page] (edit-link profile-page candidate-page "Edit" "button toggle-edit"))
  ([profile-page candidate-page text class]
   (let [profile? (= (<sub [:wh.pages.core/page]) :profile)]
     [link text (if profile? profile-page candidate-page)
      :id (when-not profile? (:id (<sub [:wh.pages.core/page-params])))
      :class class])))

(defn edit-header
  []
  (edit-link :profile-edit-header :candidate-edit-header))

(defn header-view
  [user-type data]
  [:section.profile-section.header
   (when (owner-or-admin? user-type)
     [edit-header])
   [avatar data]
   [name-view data]
   [skills data]
   [linkbox data]
   [summary data]
   [contributions user-type data]])

;; Profile header – edit

(defn predefined-avatar-picker []
  (into
   [:div.avatars]
   (for [i (range 1 6)]
     [:img.avatar {:class (when (= i (<sub [::subs/predefined-avatar])) "selected")
                   :src (common-user/avatar-url i)
                   :on-mouse-down #(dispatch [::events/set-predefined-avatar i])
                   :alt (str "Pre-set avatar image " i)}])))

(defn custom-avatar-picker []
  (if (<sub [::subs/avatar-uploading?])
    [:p "Uploading your avatar, please wait..."]
    [:div.file.avatar-picker
     [:label.file-label
      [:input.file-input {:type "file"
                          :name "avatar"
                          :on-change (upload/handler
                                      :launch [::events/image-upload]
                                      :on-upload-start [::events/avatar-upload-start]
                                      :on-success [::events/avatar-upload-success]
                                      :on-failure [::events/avatar-upload-failure])}]
      [:span.file-cta.button
       [:span.file-label "Choose a file..."]]]
     [:div.avatar [:img {:src (<sub [::subs/image-url])
                         :alt "Uploaded avatar"}]]]))

(defn avatar-field []
  (let [custom? (<sub [::subs/custom-avatar-mode])]
    [:div.field.avatar-field
     [:label.label "Your avatar"]
     [:div.control
      [:label.radio
       [:input {:type "radio" :name "avatar-type" :checked (not custom?) :on-change #(dispatch [::events/set-custom-avatar-mode false])}] " Use a predefined avatar"
       (when-not custom?
         [predefined-avatar-picker])]
      [:label.radio
       [:input {:type "radio" :name "avatar-type" :checked custom? :on-change #(dispatch [::events/set-custom-avatar-mode true])}] " Upload your own image"
       (when custom?
         [custom-avatar-picker])]]]))

(defn skill-rating [i rating]
  (let [hover-rate (reagent/atom nil)]
    (fn [i rating]
      (let [rating (or @hover-rate rating 0)]
        (into
         [:div.rating]
         (for [digit (range 1 6)]
           [icon (str "rating-" digit)
            :class (when (= digit rating) "selected")
            :on-mouse-over #(reset! hover-rate digit)
            :on-mouse-out #(reset! hover-rate nil)
            :on-mouse-down #(dispatch [::events/rate-skill i digit])]))))))

(defn cancel-link
  ([] (cancel-link "button button--small"))
  ([class]
   (let [candidate? (contains? #{:candidate-edit-header :candidate-edit-cv :candidate-edit-private} (<sub [:wh.pages.core/page]))]
     (if candidate?
       [link "Cancel" :candidate :id (:id (<sub [:wh.pages.core/page-params])) :class class]
       [link "Cancel" :profile :class class]))))

(defn skill-field [{:keys [name rating]}
                   {:keys [i label]}]
  [:div.field.skill-field
   (when label [:label.label label])
   [:div.control
    [:div.text-field-control
     [:input.input {:value name
                    :placeholder "Type in a new skill"
                    :on-change #(dispatch-sync [::events/edit-skill i (-> % .-target .-value)])}]]
    [skill-rating i rating]]])

(defn header-edit []
  [:form.wh-formx.header-edit
   [:h1 "Edit your basic info"]
   [avatar-field]
   [text-field (<sub [::subs/name])
    {:label "Your name"
     :on-change [::events/edit-name]}]
   [multi-edit
    (<sub [::subs/rated-skills])
    {:label [:div "Skills" [:br] [:div.skills "If you are just getting started it's a 1 but if you could write a book on the skill give yourself a 5."]]
     :component skill-field}]
   [multi-edit
    (<sub [::subs/editable-urls])
    {:label "Websites"
     :placeholder "Link to your web page, profile, portfolio, etc..."
     :on-change [::events/edit-url]}]
   [text-field (<sub [::subs/summary]) {:type :textarea
                                        :label "Summary"
                                        :placeholder "Here's your chance to tell us a bit about you. Use this summary as your personal elevator pitch, what have you achieved and what makes you tick."
                                        :on-change [::events/edit-summary]}]
   [error-box]
   [:div.buttons-container.is-flex
    [:button.button.button--small {:on-click #(do (.preventDefault %)
                                                  (dispatch [::events/save-header]))} "Save"]
    [cancel-link]]])

;; CV section – edit link

(defn cv-section-edit-link []
  [:form.wh-formx.cv-edit.wh-formx__layout
   [:h1 "Edit your resume"]
   [text-field (<sub [::subs/cv-link]) {:label "Resume link" :on-change [::events/edit-cv-link]}]
   [error-box]
   [:div.buttons-container
    [:button.button.button--small {:on-click #(do (.preventDefault %)
                                                  (dispatch [::events/save-cv-info]))} "Save"]
    [cancel-link]]])

;; CV section – view

(defn cv-section-view
  ([opts] (cv-section-view :owner opts))
  ([user-type {:keys [cv-link cv-filename cv-url]}]
   [:section.profile-section.cv
    [:div.cv__view
     [:h2 "Career History"]
     [error-box]
     (when cv-url
       [:p (if (owner? user-type) "You uploaded " "Uploaded CV: ")
        [:a.a--underlined {:href cv-url, :target "_blank", :rel "noopener"} cv-filename]])
     (when cv-link
       [:p (if (owner? user-type) "Your external resume: " "External resume: ")
        [:a.a--underlined {:href cv-link, :target "_blank", :rel "noopener"} cv-link]])
     (when-not (or cv-url cv-link)
       (if (owner? user-type)
         "You haven't uploaded resume yet."
         "No uploaded resume yet."))]
    (when (owner-or-admin? user-type)
      [:div.cv__buttons
       (if (<sub [::subs/cv-uploading?])
         [:button.button {:disabled true} "Uploading..."]
         [:label.file-label.cv__upload
          [:input.file-input {:type "file"
                              :name "avatar"
                              :on-change (upload/handler
                                          :launch [::events/cv-upload]
                                          :on-upload-start [::events/cv-upload-start]
                                          :on-success [::events/cv-upload-success]
                                          :on-failure [::events/cv-upload-failure])}]
          [:span.file-cta.button
           [:span.file-label "Upload resume"]]])
       [edit-link :profile-edit-cv :candidate-edit-cv "Add a link to resume" "button"]])]))

;; Private section – view

(defn itemize [items & {:keys [class no-data-message]}]
  (if (seq items)
    (into [:ul {:class class}]
          (for [item items]
            [:li item]))
    no-data-message))

(defn view-field [label content]
  [:div.private__field
   [:label.private__label label]
   [:div.private__content content]])

(defn email-link [email]
  [:a {:href (str "mailto:" email)} email])

(defn successful-save-info []
  [:article.message.is-success
   [:div.message-header
    [:p "Success"]
    [:button.delete {:on-click #(dispatch [::events/clear-url-save-success])}]]
   [:div.message-body
    "Settings saved successfully."]])

(defn private-section-view
  ([opts] (private-section-view :owner opts))
  ([user-type {:keys [email job-seeking-status company-perks role-types remote
                      salary visa-status current-location
                      preferred-locations fields title email-link-fn]
               :or   {fields #{:email :status :traits :salary :visa :remote :preferred-types :current-location :preferred-locations}
                      title "Preferences" email-link-fn email-link}}]
   [:section.profile-section.private
    (when (owner-or-admin? user-type)
      [edit-link :profile-edit-private :candidate-edit-private])
    [:h2.private__disclaimer title]
    (when (<sub [::subs/url-save-success])
      [successful-save-info])
    (when (owner? user-type)
      [:div "This section is for our info only — we won’t show this directly to anyone 🔐"])
    (when (:email fields)               [view-field "Email:" [email-link-fn email]])
    (when (:status fields)              [view-field "Status:" job-seeking-status])
    (when (:traits fields)              [view-field "Company traits:" (itemize company-perks :no-data-message "No perks selected.")])
    (when (:salary fields)              [view-field "Expected salary:" salary])
    (when (:visa fields)                [view-field "Visa status:" visa-status])
    (when (:remote fields)              [view-field "Prefer remote working:" (if remote "Yes" "No")])
    (when (:preferred-types fields)     [view-field "Preferred role types:" (itemize (map #(str/replace % #"_" " ") role-types) :no-data-message "None")])
    (when (:current-location fields)    [view-field "Current location:" (or current-location "None")])
    (when (:preferred-locations fields) [view-field "Preferred locations:" (itemize preferred-locations :no-data-message "No locations selected.")])]))

;; Private section – edit

(defn current-location-field []
  [text-field (<sub [::subs/current-location-label])
   {:label "Current location"
    :suggestions (<sub [::subs/current-location-suggestions])
    :on-change [::events/edit-current-location]
    :on-select-suggestion [::events/select-current-location-suggestion]
    :placeholder "Type to search location..."}])

(defn preferred-location-field [value {:keys [i label]}]
  [text-field value
   {:label label
    :suggestions (<sub [::subs/suggestions i])
    :on-change [::events/edit-preferred-location i]
    :on-select-suggestion [::events/select-suggestion i]
    :on-remove [::events/remove-preferred-location i]
    :placeholder "Type to search location..."}])

(defn private-section-edit []
  [:form.wh-formx.private-edit.wh-formx__layout
   [:h1 "Edit your private data"]
   [text-field (<sub [::subs/email])
    {:label "Your email"
     :validate ::p/email
     :on-change [::events/edit-email]}]
   [select-field (<sub [::subs/job-seeking-status])
    {:options (<sub [::subs/job-seeking-statuses])
     :label "Job seeking status"
     :on-change [::events/edit-job-seeking-status]}]
   [multi-edit
    (<sub [::subs/company-perks])
    {:label "Company perks"
     :on-change [::events/edit-perk]
     :placeholder "Type what you're looking for"}]
   [field-container
    {:label "Salary" :class "private-edit__salary"}
    [:div.columns
     [:div.column
      [:div.text-field-control
       [text-input (<sub [::subs/salary-min])
        {:type :number
         :placeholder "Minimum"
         :on-change   [::events/edit-salary-min]}]]]
     [:div.column
      [select-input (<sub [::subs/salary-currency])
       {:options   (<sub [::subs/currencies])
        :on-change [::events/edit-salary-currency]}]]
     [:div.column
      [select-input (<sub [::subs/salary-time-period])
       {:options   (<sub [::subs/time-periods])
        :on-change [::events/edit-salary-time-period]}]]]]

   [field-container
    {:label "Visa status"}
    [multiple-buttons (<sub [::subs/visa-status])
     {:options (<sub [::subs/visa-statuses])
      :on-change [::events/toggle-visa-status]}]]
   (when (<sub [::subs/visa-status-other-visible?])
     [text-field (<sub [::subs/visa-status-other])
      {:label "Other Visa status"
       :on-change [::events/edit-visa-status-other]}])
   [field-container
    {:label "Role types"}
    [multiple-buttons (<sub [::subs/role-types])
     {:options [{:id "Full_time", :label "Full time"} "Contract" "Intern"]
      :on-change [::events/toggle-role-type]}]]
   [current-location-field]
   [multi-edit (<sub [::subs/preferred-location-labels])
    {:label "Preferred locations"
     :component preferred-location-field}]
   [labelled-checkbox
    (<sub [::subs/remote])
    {:label "Prefer remote work"
     :on-change [::events/set-remote]}]
   [error-box]
   [:div.buttons-container
    [:button.button.button--small {:on-click #(do (.preventDefault %)
                                                  (dispatch [::events/save-private]))} "Save"]
    [cancel-link]]])

(defn company-user-view
  [{:keys [email name]}]
  [:section.profile-section.private
   [link "Edit" :profile-edit-company-user :class "button toggle-edit"]
   [:h2.private__disclaimer "Profile"]
   [view-field "Name:" name]
   [view-field "Email:" [email-link email]]])

(defn company-user-edit []
  [:form.wh-formx.private-edit.wh-formx__layout
   [:h1 "Edit your profile"]
   [text-field (<sub [::subs/email])
    {:label "Your email"
     :validate ::p/email
     :on-change [::events/edit-email]}]
   [text-field (<sub [::subs/name])
    {:label "Your name"
     :validate ::p/non-empty-string
     :on-change [::events/edit-name]}]
   [error-box]
   [:div.buttons-container
    [:button.button.button--small {:on-click #(do (.preventDefault %)
                                    (dispatch [::events/save-company-user]))} "Save"]
    [cancel-link]]])

(defn main-view []
  [:div
   (if (<sub [:user/company?])
     [company-user-view (<sub [::subs/company-data])]
     [:div
      [header-view :owner (<sub [::subs/header-data])]
      [cv-section-view (<sub [::subs/cv-data])]
      [private-section-view (<sub [::subs/private-data])]])
   [:a.button
    {:data-pushy-ignore "true"
     :href (routes/path :logout)}
    "Logout"]])

(defn view-page []
  [:div.main-container
   [:div.main.profile [main-view]]])

(defn header-edit-page []
  [:div.main-container
   [:div.main.profile
    [:div.wh-formx-page-container
     [:div.columns.is-variable.is-2
      [:div.column.is-7
       [header-edit]]]]]])

(defn cv-edit-page []
  [:div.main-container
   [:div.main.profile
    [:div.wh-formx-page-container
     [:div.columns.is-variable.is-2
      [:div.column.is-7
       [cv-section-edit-link]]]]]])

(defn private-edit-page []
  [:div.main-container
   [:div.main.profile
    [:div.wh-formx-page-container
     [:div.columns.is-variable.is-2
      [:div.column.is-7
       [private-section-edit]]]]]])

(defn company-user-edit-page []
  [:div.main-container
   [:div.main.profile
    [:div.wh-formx-page-container
     [:div.columns.is-variable.is-2
      [:div.column.is-7
       [company-user-edit]]]]]])

(defn improve-recommendations-page []
  [:div.main-container
   [:div.main.profile
    [:div.wh-formx-page-container
     [:div.columns.is-variable.is-2
      [:div.column.is-7
       [:form.wh-formx.header-edit
        [:h1 "Improve recommendations"]
        [:h3 "To improve or change your recommendations please add/remove skills and update or add multiple locations."]
        [multi-edit
         (<sub [::subs/rated-skills])
         {:label     [:div "Skills" [:br] [:div.skills "If you are just getting started it's a 1 but if you could write a book on the skill give yourself a 5."]]
          :component skill-field}]
        [multi-edit (<sub [::subs/preferred-location-labels])
         {:label     "Preferred locations"
          :component preferred-location-field}]
        [labelled-checkbox
         (<sub [::subs/remote])
         {:label     "Prefer remote work"
          :on-change [::events/set-remote]}]
        [error-box]
        [:div.buttons-container
         [:button.button.button--small
          {:on-click #(do (.preventDefault %)
                          (dispatch [::events/save-recommendations]))} "Save"]]]]]]]])
