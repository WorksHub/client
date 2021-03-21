(ns wh.logged-in.profile.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch dispatch-sync]]
            [reagent.core :as reagent]
            [wh.common.specs.primitives :as p]
            [wh.common.text :refer [pluralize]]
            [wh.common.upload :as upload]
            [wh.common.user :as common-user]
            [wh.components.button-auth :as button-auth]
            [wh.components.common :refer [link link-user]]
            [wh.components.forms.views :refer [field-container
                                               toggle
                                               labelled-checkbox
                                               multi-edit multiple-buttons
                                               select-field select-input
                                               text-field text-input]]
            [wh.components.icons :refer [icon]]
            [wh.logged-in.profile.components :as components]
            [wh.logged-in.profile.components.cover-letter :as cover-letter]
            [wh.logged-in.profile.components.cv :as cv]
            [wh.logged-in.profile.events :as events]
            [wh.logged-in.profile.subs :as subs]
            [wh.profile.db :as profile]
            [wh.profile.section-admin.core :as section-admin]
            [wh.profile.update-private.events :as edit-private-events]
            [wh.profile.update-private.subs :as edit-private-subs]
            [wh.profile.update-private.views :as edit-private]
            [wh.profile.update-public.events :as edit-modal-events]
            [wh.profile.update-public.views :as edit-modal]
            [wh.routes :as routes]
            [wh.styles.profile :as styles]
            [wh.subs :refer [<sub]]
            [wh.util :as util]))

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

(defn linkbox [{:keys [other-urls stackoverflow-info]}]
  (when (seq other-urls)
    (let [so-reputation (:reputation stackoverflow-info)]
      (into
        [:div.linkbox]
        (for [{:keys [url type display]} other-urls]
          [:span.linkbox__link-wrapper
           [icon (name type)]
           (when (and (= type :stackoverflow)
                      so-reputation)
             [:span.linkbox__reputation-badge so-reputation])
           [:a.linkbox__link {:href   url
                              :target "_blank"
                              :rel    "noopener"} display]])))))

(defn owner? [user-type] (= user-type :owner))
(defn admin? [user-type] (= user-type :admin))
(defn owner-or-admin? [user-type] (contains? #{:owner :admin} user-type))

(defn connect-buttons
  [user-type
   {:keys [stackoverflow-info github-id twitter-info]}]
  (when (and (owner? user-type)
             (or (not stackoverflow-info)
                 (not github-id)
                 (not twitter-info)))
    [:div
     (when-not stackoverflow-info
       [button-auth/connect-button :stackoverflow])
     (when-not github-id
       [button-auth/connect-button :github])
     (when-not twitter-info
       [button-auth/connect-button :twitter])]))

(defn summary [opts]
  (let [summary (:summary opts)]
    (when-not (str/blank? summary)
      [:div.summary summary])))

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
         [link "Create Article" :contribute :class "button"]]))))

(defn edit-link
  ([profile-page candidate-page] (edit-link profile-page candidate-page "Edit" "button toggle-edit"))
  ([profile-page candidate-page text class]
   (let [profile? (= (<sub [:wh.pages.core/page]) :profile)]
     [link text (if profile? profile-page candidate-page)
      :id (when-not profile? (:id (<sub [:wh/page-params])))
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
   [connect-buttons user-type data]
   [linkbox data]
   [summary data]
   [contributions user-type data]])

;; Profile header – edit

(defn- rating-points [i rating hover-rate active]
  (for [digit (range 1 6)]
    (if active
      [icon (str "rating-" digit)
       :class (when (= digit rating) "selected")
       :on-mouse-over #(reset! hover-rate digit)
       :on-mouse-out #(reset! hover-rate nil)
       :on-mouse-down #(dispatch [::events/rate-skill i digit])]

      [icon (str "rating-" digit)])))

(defn skill-rating [i rating active]
  (let [hover-rate (reagent/atom nil)]
    (fn [i rating active]
      (let [rating (or @hover-rate rating 0)]
        (into
          [:div.rating]
          (rating-points i rating hover-rate active))))))

(defn cancel-link
  ([] (cancel-link "button button--small"))
  ([class]
   (let [candidate? (contains? #{:candidate-edit-header :candidate-edit-private}
                               (<sub [:wh.pages.core/page]))]
     (if candidate?
       [link-user "Cancel" (<sub [:user/admin?]) :id (:id (<sub [:wh/page-params])) :class class]
       [link "Cancel" :profile :class class]))))

(defn skill-field [{:keys [name rating]}
                   {:keys [i label]}]
  (let [active-rating (not (str/blank? name))]
    [:div.field.skill-field
     (when label [:label.label label])
     [:div.control
      [:div.text-field-control
       [:input.input {:value       name
                      :placeholder "Type in a new skill"
                      :data-test   "skill"
                      :on-change   #(dispatch-sync
                                      [::events/edit-skill i
                                       (-> % .-target .-value)])}]]
      [skill-rating i rating active-rating]]]))

(defn header-edit []
  [:form.wh-formx.header-edit
   [:h1 "Edit your skills"]
   [multi-edit
    (<sub [::subs/rated-skills])
    {:label     [:div "Skills"
                 [:br]
                 [:div.skills "If you are just getting started it's a 1 but if you could write a book on the skill give yourself a 5."]]
     :component skill-field}]
   [:div.buttons-container.is-flex
    [:button.button.button--small {:data-test "save"
                                   :on-click  #(do (.preventDefault %)
                                                   (dispatch [::events/save-header]))} "Save"]
    [cancel-link]]])

;; CV section – edit link

(defn upload-document [document-name uploading? on-change]
  [:div.profile-section__upload-document
   (if uploading?
     [:button.button {:disabled true} "Uploading..."]
     [:label.file-label.profile-section__upload-document__label
      [:input.file-input {:type      "file"
                          :name      "avatar"
                          :on-change on-change}]
      [:span.file-cta.button
       [:span.file-label (str "Upload " document-name)]]])])

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

;; Private section – edit

(defn current-location-field []
  [text-field (<sub [::subs/current-location-label])
   {:label                "Current location"
    :data-test            "current-location"
    :suggestions          (<sub [::subs/current-location-suggestions])
    :on-change            [::events/edit-current-location]
    :on-select-suggestion [::events/select-current-location-suggestion]
    :placeholder          "Type to search location..."}])

(defn preferred-location-field [value {:keys [i label]}]
  [text-field value
   {:label                label
    :suggestions          (<sub [::subs/suggestions i])
    :on-change            [::events/edit-preferred-location i]
    :on-select-suggestion [::events/select-suggestion i]
    :on-remove            [::events/remove-preferred-location i]
    :placeholder          "Type to search location..."}])

(defn private-section-edit []
  [:form.wh-formx.private-edit.wh-formx__layout
   [:h1 "Edit your private data"]
   [text-field (<sub [::subs/email])
    {:label     "Your email"
     :validate  ::p/email
     :on-change [::events/edit-email]}]
   [text-field (<sub [::subs/phone])
    {:label     "Your phone number (inc. dialling code)"
     :on-change [::events/edit-phone]}]
   [select-field (<sub [::subs/job-seeking-status])
    {:options   (<sub [::subs/job-seeking-statuses])
     :label     "Job seeking status"
     :on-change [::events/edit-job-seeking-status]}]
   [multi-edit
    (<sub [::subs/company-perks])
    {:label       "Company perks"
     :on-change   [::events/edit-perk]
     :placeholder "Type what you're looking for"}]
   [field-container
    {:label "Salary" :class "private-edit__salary"}
    [:div.columns
     [:div.column
      [:div.text-field-control
       [text-input (<sub [::subs/salary-min])
        {:type        :number
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
     {:options   (<sub [::subs/visa-statuses])
      :on-change [::events/toggle-visa-status]}]]
   (when (<sub [::subs/visa-status-other-visible?])
     [text-field (<sub [::subs/visa-status-other])
      {:label     "Other Visa status"
       :on-change [::events/edit-visa-status-other]}])
   [field-container
    {:label "Role types"}
    [multiple-buttons (<sub [::subs/role-types])
     {:options   [{:id "Full_time", :label "Full time"} "Contract" "Intern"]
      :on-change [::events/toggle-role-type]}]]
   [current-location-field]
   [multi-edit (<sub [::subs/preferred-location-labels])
    {:label     "Preferred locations"
     :component preferred-location-field}]
   [labelled-checkbox
    (<sub [::subs/remote])
    {:label     "Prefer remote work"
     :on-change [::events/set-remote]}]
   [:div.buttons-container
    [:button.button.button--small {:data-test "save"
                                   :on-click  #(do (.preventDefault %)
                                                   (dispatch [::events/save-private]))} "Save"]
    [cancel-link]]])

(defn company-user-edit []
  [:form.wh-formx.private-edit.wh-formx__layout
   [:h1 "Edit your profile"]
   [text-field (<sub [::subs/email])
    {:label     "Your email"
     :validate  ::p/email
     :on-change [::events/edit-email]}]
   [text-field (<sub [::subs/name])
    {:label     "Your name"
     :validate  ::p/non-empty-string
     :on-change [::events/edit-name]}]
   [:div.buttons-container
    [:button.button.button--small {:on-click #(do (.preventDefault %)
                                                  (dispatch [::events/save-company-user]))} "Save"]
    [cancel-link]]])

(defn header-edit-page []
  [:div.main-container
   [:div.main.profile
    [:div.wh-formx-page-container
     [:div.columns.is-variable.is-2
      [:div.column.is-7
       [header-edit]]]]]])

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

;; -----------------------------------------------------------------------------------------------------------

(defn cv-section-view
  ([opts] (cv-section-view :owner opts))
  ([user-type {:keys [cv-link cv-filename cv-url]}]
   [:section.profile-section.cv
    [:div.cv__view
     [:h2 "Career History"]

     (when cv-url
       [:p (if (owner? user-type) "You uploaded " "Uploaded CV: ")
        [:a.a--underlined {:href cv-url, :target "_blank", :rel "noopener"}
         (if (owner? user-type) cv-filename "Click here to download")]])

     (when cv-link
       [:p (if (owner? user-type) "Your external CV: " "External CV: ")
        [:a.a--underlined
         {:href cv-link :target "_blank" :rel "noopener" :data-test "cv-link"}
         cv-link]])

     (when-not (or cv-url cv-link)
       (if (owner? user-type)
         "You haven't uploaded cv yet."
         "No uploaded cv yet."))]

    (when (owner-or-admin? user-type)
      [:div.cv__upload
       (conj
         (upload-document "resume" (<sub [::subs/cv-uploading?])
                          (upload/handler
                            :launch [::events/cv-upload]
                            :on-upload-start [::events/cv-upload-start]
                            :on-success [::events/cv-upload-success]
                            :on-failure [::events/cv-upload-failure])))])]))

(defn private-section-view
  ([opts] (private-section-view :owner opts))
  ([user-type {:keys [email phone job-seeking-status company-perks role-types remote
                      salary visa-status current-location
                      preferred-locations fields title email-link-fn]
               :or   {fields #{:email :phone :status :traits :salary :visa :remote :preferred-types :current-location :preferred-locations}
                      title  "Preferences" email-link-fn email-link}}]
   [:section.profile-section.private
    (when (owner-or-admin? user-type)
      [edit-link :profile-edit-private :candidate-edit-private])
    [:h2.private__disclaimer title]
    (when (<sub [::subs/url-save-success])
      [successful-save-info])
    (when (owner? user-type)
      [:div "This section is for our info only — we won’t show this directly to anyone 🔐"])
    (when (:email fields)               [view-field "Email:" [email-link-fn email]])
    (when (:phone fields)               [view-field "Phone Number:" phone])
    (when (:status fields)              [view-field "Status:" job-seeking-status])
    (when (:traits fields)              [view-field "Company traits:" (itemize company-perks :no-data-message "No perks selected.")])
    (when (:salary fields)              [view-field "Expected salary:" salary])
    (when (:visa fields)                [view-field "Visa status:" visa-status])
    (when (:remote fields)              [view-field "Prefer remote working:" (if remote "Yes" "No")])
    (when (:preferred-types fields)     [view-field "Preferred role types:" (itemize (map #(str/replace % #"_" " ") role-types) :no-data-message "None")])
    (when (:current-location fields)    [view-field "Current location:" (or current-location "None")])
    (when (:preferred-locations fields) [view-field "Preferred locations:" (itemize preferred-locations :no-data-message "No locations selected.")])]))

;; -----------------------------------------------------------------------------------------------------------


(defn edit-user-private-info-wrapper
  [user-type opts]
  [components/editable-section
   {:editable? (owner? user-type)
    :editing?  (<sub [::edit-private-subs/editing-profile?])
    :on-edit   [::edit-private-events/open-form]
    :read-body [components/edit-user-private-info user-type opts]
    :data-test :toggle-edit-private-info
    :display-toggle? (<sub [::subs/display-toggle?])
    :edit-body [:<>
                [components/sec-title "Edit profile"]
                [edit-private/profile-edit-inline]]}])

(defn cover-letter-section-view-new
  [user-type {:keys [cover-letter-filename cover-letter-url]}]
  [components/section-custom
   {:data-test "section-cover-letter"}
   [components/sec-title "Default Cover Letter"]

   (when cover-letter-url
     [:p (if (owner? user-type) "You uploaded " "Uploaded Cover Letter: ")
      [components/resource {:href cover-letter-url
                            :text (if (owner? user-type) cover-letter-filename "Click here to download")}]])

   (when-not cover-letter-url
     (if (owner? user-type)
       "You haven't uploaded Cover Letter yet"
       "No uploaded Cover Letter yet"))

   (when (owner-or-admin? user-type)
     [components/section-buttons
      [components/upload-button {:document   "cover letter"
                                 :data-test  "upload-cover-letter"
                                 :uploading? (<sub [::subs/cover-letter-uploading?])
                                 :on-change  (upload/handler
                                               :launch [::events/cover-letter-upload]
                                               :on-upload-start [::events/cover-letter-upload-start]
                                               :on-success [::events/cover-letter-upload-success]
                                               :on-failure [::events/cover-letter-upload-failure])}]
      (when (and cover-letter-url (owner? user-type))
        [components/small-button
         {:on-click #(dispatch [::events/remove-cover-letter])}
         "Remove cover letter"])])])

(defn- cv-link-section [cv-link user-type]
  (let [editing-cv-link? (<sub [::subs/editing-cv-link?])]
    [:div
     (if editing-cv-link?
       (let [cv-link-val (<sub [::subs/cv-link-editable])
             save-link!  #(dispatch [::events/save-cv-info
                                     {:type    :update-cv-link
                                      :cv-link cv-link-val}])]
         [:div (util/smc styles/cta__text-field-container)
          [components/text-field cv-link-val
           {:on-change   [::events/edit-cv-link-editable]
            :on-enter    save-link!
            :placeholder "Enter link to resume"
            :data-test   "enter-cv-link"
            :class       styles/cta__text-field}]

          [components/small-button {:on-click  save-link!
                                    :data-test "save-cv-link"}
           "Save"]])

       (when cv-link
         [:p (util/smc styles/cv-link)
          (if (owner? user-type) "Your external CV: " "External CV: ")
          [components/resource {:href      cv-link
                                :text      cv-link
                                :data-test "cv-link"}]]))]))

(defn cv-section-view-new
  [user-type {:keys [cv-link cv-filename cv-url]}]
  [components/section-custom
   {:data-test "section-career-history"}
   [components/sec-title "Career History"]

   (when (or cv-url cv-link)
     [:div (when cv-url
             [:p {:data-test "upload-resume-success"}
              (if (owner? user-type) "You uploaded " "Uploaded CV: ")
              [components/resource {:href cv-url
                                    :text (if (owner? user-type) cv-filename "Click here to download")}]])


      [cv-link-section cv-link user-type]])

   (when-not (or cv-url cv-link)
     (if (owner? user-type)
       "You haven't uploaded CV yet"
       "No uploaded cv yet"))

   (when (owner-or-admin? user-type)
     (let [editing-cv-link? (<sub [::subs/editing-cv-link?])]
       [components/section-buttons
        (when-not editing-cv-link?
          [components/upload-button {:document   "CV"
                                     :data-test  "upload-resume"
                                     :uploading? (<sub [::subs/cv-uploading?])
                                     :on-change  (upload/handler
                                                   :launch [::events/cv-upload]
                                                   :on-upload-start [::events/cv-upload-start]
                                                   :on-success [::events/cv-upload-success]
                                                   :on-failure [::events/cv-upload-failure])}])

        [components/small-link
         {:on-click  #(dispatch [::events/toggle-cv-link-editing cv-link])
          :inverted? editing-cv-link?
          :data-test "change-cv-link"
          :text      (cond
                       (and cv-link editing-cv-link?) "Cancel"
                       cv-link                        "Change link to CV"
                       :else                          "Add link to CV")}]]))])


(defn section-public-access-settings []
  (let [profile-public? (<sub [::subs/published?])
        id              (<sub [::subs/id])]
    [components/section-custom {:type :highlighted}
     [:div {:class styles/access-settings}

      [:div {:class styles/access-settings__description}
       [:div {:class styles/access-settings__title-wrapper}
        [:div {:class styles/access-settings__title}
         "Published"]
        [toggle {:value     profile-public?
                 :on-change #(dispatch [::events/toggle-profile-visibility])
                 :data-test "toggle-profile-visibility"}]]
       [:span (if profile-public?
                [:span "Your profile is visible to everyone. " [components/underline-link
                                                                {:href (routes/path :user :params {:id id})
                                                                 :text "See it live!"
                                                                 :data-test :public-profile-link}]]
                "Your profile is hidden from everyone. Click the toggle to make it visible.")]]]]))

(defn main-view []
  (let [is-company?          (<sub [:user/company?])
        is-owner?            (<sub [::subs/owner?])
        contributions?       (boolean (<sub [::subs/contributions-collection]))
        issues               (<sub [::subs/issues])
        contributions        (<sub [::subs/contributions])
        cover-letter-data    (<sub [::subs/cover-letter-data])
        {:keys
         [cover-letter-url]} cover-letter-data
        cover-present?       cover-letter-url
        private-data         (<sub [::subs/private-data])
        cv-data              (<sub [::subs/cv-data])
        {:keys
         [cv-link cv-url]}   cv-data
        cv-present?          (or cv-url cv-link)]
    [components/content
     [edit-modal/profile-edit-modal]
     [section-public-access-settings]
     [section-admin/section-for-admin]
     [components/section-stats {:is-owner?      is-owner?
                                :percentile     (<sub [::subs/percentile])
                                :created        (<sub [::subs/created])
                                :articles-count (<sub [::subs/articles-count])
                                :issues-count   (<sub [::subs/issues-count])}]
     (when-not is-company?
       (if cv-present?
         [cv-section-view-new :owner cv-data]
         [cv/cv-cta]))

     (when-not is-company?
       (if cover-present?
         [cover-letter-section-view-new :owner cover-letter-data]
         [cover-letter/cover-letter-cta]))

     (when-not is-company?
       [edit-user-private-info-wrapper :owner private-data])

     [components/section-skills {:type                   :private
                                 :skills                 (<sub [::subs/skills])
                                 :interests              (<sub [::subs/interests])
                                 :query-params           (<sub [:wh/query-params])
                                 :max-skills             profile/maximum-skills
                                 ;; Edit settings below this point
                                 :changes?               (<sub [::subs/edit-tech-changes?])
                                 :editing?               (<sub [::subs/editing-tech?])
                                 :on-edit                [::events/toggle-edit-tech]
                                 :on-cancel              [::events/toggle-edit-tech]
                                 :on-skill-rating-change [::events/on-skill-rating-change]
                                 :on-save                [::events/on-save-edit-tech]
                                 :skills-search          {:search-term  (<sub [::subs/skills-search])
                                                          :id           "profile-edit-tech-experience"
                                                          :placeholder  "Search and add up to 6 technologies"
                                                          :tags         (<sub [::subs/skills-search-results])
                                                          :on-change    [::events/set-skills-search]
                                                          :on-tag-click #(dispatch [::events/toggle-skill %])}
                                 ;;
                                 :interests-search       {:search-term  (<sub [::subs/interests-search])
                                                          :id           "profile-edit-tech-interests"
                                                          :placeholder  "Search and add your interests"
                                                          :tags         (<sub [::subs/interests-search-results])
                                                          :on-change    [::events/set-interests-search]
                                                          :on-tag-click #(dispatch [::events/toggle-interest %])}}]
     (if contributions?
       [components/section-contributions
        (<sub [::subs/contributions-calendar])
        (<sub [::subs/contributions-count])
        (<sub [::subs/contributions-repos])
        (<sub [::subs/contributions-months])]

       [components/connect-gh-cta])

     (if (seq contributions)
       [components/section-articles contributions :private]
       [components/articles-cta])

     (if (seq issues)
       [components/section-issues issues :private]
       [components/oss-cta])]))

(defn view-page []
  [components/container
   [components/profile (<sub [::subs/header-data])
    {:twitter         (<sub [::subs/url :twitter])
     :stackoverflow   (<sub [::subs/url :stackoverflow])
     :github          (<sub [::subs/url :github])
     :website         (<sub [::subs/url :web])
     :last-seen       (<sub [::subs/last-seen])
     :updated         (<sub [::subs/updated])
     :display-toggle? (<sub [::subs/display-toggle?])
     :on-edit         #(dispatch [::edit-modal-events/open-modal])}
    :private]
   [main-view]])
