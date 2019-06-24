(ns wh.company.candidate.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch]]
    [wh.company.applications.views :refer [get-in-touch-overlay application-buttons job-selection-overlay]]
    [wh.company.candidate.events :as events]
    [wh.company.candidate.subs :as subs]
    [wh.components.common :refer [link]]
    [wh.components.error.views :refer [loading-error]]
    [wh.components.icons :refer [icon]]
    [wh.logged-in.profile.views :as profile-views :refer [view-field itemize]]
    [wh.subs :refer [<sub]]))

(defn itemize-jobs [jobs]
  (if (seq jobs)
    (itemize (for [{:keys [id slug title company]} jobs]
               [link (str title " – " (:name company)) :job :slug slug :class "a--underlined"]))
    "None"))

(defn approval->str [{:keys [source time status]}]
  (str (str/capitalize (or status "")) (when source (str " by " source)) (when time (str " on " time))))

(defn state->str
  [s]
  (case s
    "get_in_touch" "Interviewing"
    "approved"     "Pending"
    "rejected"     "Rejected by WorksHub"
    "hired"        "Hired"
    (str/capitalize s)))

(defn approve-buttons [{:keys [approval id email updating]}]
  [:div
   [:button.button.button--green.button--small
    {:disabled (= (:status approval) "approved")
     :title (approval->str approval)
     :class (when (= "approved" updating) "button--loading")
     :on-click #(do (.preventDefault %)
                    (dispatch [:candidate/set-approval-status id email "approved"]))} "Approve"]
   [:button.button.button--small
    {:disabled (= (:status approval) "rejected")
     :title (approval->str approval)
     :class (when (= "rejected" updating) "button--light button--loading")
     :on-click #(do (.preventDefault %)
                    (dispatch [:candidate/set-approval-status id email "rejected"]))} "Reject"]])

(defn admin-section [{:keys [applied likes hs-url type company]}]
  [:section.profile-section.admin.private
   [:div.spread-or-stack
    [:h2 "Admin section"]
    [approve-buttons (<sub [::subs/approve-data])]]
   (when (= type "company")
     [view-field "Company:" [link (:name company) :company-dashboard :id (:id company) :class "a--underlined"]])
   [view-field "Hubspot:" (if hs-url
                            [:a.a--underlined {:href   hs-url
                                               :target "_blank"
                                               :rel    "noopener"} hs-url]
                            "No profile yet")]
   (when-not (= type "company")
     [view-field "Applied jobs:" (itemize-jobs (map :job applied))])
   (when-not (= type "company")
     [view-field "Liked jobs:" (itemize-jobs likes)])])

(defn set-application-state!
  [id-or-ids state]
  (if (sequential? id-or-ids)
    #(dispatch [::events/show-job-selection-overlay (name state)])
    #(dispatch [::events/set-application-state id-or-ids (name state)])))

(defn applications-section
  []
  [:section.profile-section.profile-section__applications
   [:h2 "Applications at " (:name (<sub [:wh.user.subs/company]))]
   (for [{:keys [job timestamp state note]} (<sub [::subs/application-data])]
     ^{:key (:id job)}
     [:div.profile-section__applications__job
      [:div.columns
       {:class (str "profile-section__applications__job--state-" state)}
       [:div.column
        [:div.profile-section__applications__job-link
         [link (:title job) :job :slug (:slug job)]]
        [:div.profile-section__applications__applied-on "Applied on " (first (str/split timestamp #"T"))]]
       [:div.column.is-narrow.profile-section__applications__actions
        [:div.profile-section__applications__state (state->str state)]]]
      (when note
        [:div.profile-section__applications__note note])])])

(defn login-prompt
  []
  [:div.main
   [:div.container
    [:h1 "Login"]
    [:h2 "to continue to this page"]
    [:div.container.login-buttons
     [:div.container
      [:button.button.button--large.button--light {:on-click #(dispatch [::events/go-to-magic-form])}
       [icon "magic-link" :class "button__icon button__icon--light"]
       "Login"]]
     [:div.container
      [:h3 "If you do not have account, please contact your company manager"
       [:br]
       "or talk to us now via live chat."]]
     [:img.sparkle {:src "/images/sparkle.svg"
                    :alt "Background sparkle"}]]]])

(defn application-buttons-on-header
  []
  (let [applications (<sub [::subs/application-data])
        multiple?    (> (count applications) 1)
        states       (when multiple? (set (map :state applications)))
        updating?    (<sub [::subs/updating?])]
    (when-not (or (zero? (count applications))
                  (and multiple? (every? #{"hired"} states)))
      [:section.profile-section.profile-section__application-buttons-container
       {:class (when updating? "profile-section__application-buttons-container--updating")}
       (if updating?
         [:div.is-loading-spinner]
         [application-buttons
          :class "profile-section__applications__buttons"
          :state (if multiple?
                   (let [states (disj states "hired")]
                     (cond (every? #{"get_in_touch"} states)    "get_in_touch"
                           (every? #{"rejected" "pass"} states) "rejected"
                           :else                                "pending"))
                   (-> applications (first) :state))
          :on-hire (if multiple?
                     (set-application-state! (->> applications
                                                  (remove #(not= "get_in_touch" (:state %)))
                                                  (map (comp :id :job))) :hire)
                     (set-application-state! (-> applications (first) :job :id) :hire))
          :on-get-in-touch (if multiple?
                             (set-application-state! (map (comp :id :job) applications) :get_in_touch)
                             (set-application-state! (-> applications (first) :job :id) :get_in_touch))
          :on-pass (if multiple?
                     (set-application-state! (map (comp :id :job) applications) :pass)
                     (set-application-state! (-> applications (first) :job :id) :pass))
          :could-hire? (contains? states "get_in_touch")])])))

(defn page []
  [:div.main-container
   (let [error (<sub [::subs/loading-error])
         header-data (<sub [::subs/header-data])
         private-data (<sub [::subs/private-data])
         company? (<sub [:user/company?])
         user-type (if company? :company :admin)]
     (cond
       (= error :unauthorised)
       [:div.main.profile
        [:div.container
         [:h1 "Not authorised"]
         [:div
          [:p "You are not authorised to see this page, try logging in."]]]]

       (= error :user-not-found)
       [:div.main.profile
        [:div.container
         [:h1 "User not found"]
         [:div
          [:p "Please check that user id in url is correct."]]]]

       (= error :unknown-error)
       [:div.main.profile
        [loading-error]]

       (<sub [::subs/show-login?])
       [login-prompt]

       :else
       [:div.main.profile
        [profile-views/header-view user-type header-data]
        (when company? [application-buttons-on-header])
        (when (<sub [:user/admin?]) [admin-section (<sub [::subs/admin-data])])
        (when company? [applications-section])
        [profile-views/cv-section-view user-type (<sub [::subs/cv-data])]
        [profile-views/private-section-view user-type (merge private-data
                                                             {:title "Details"}
                                                             (when (<sub [:user/company?])
                                                               {:fields (<sub [::subs/profile-fields])}))]
        (when (<sub [::subs/show-get-in-touch-overlay?])
          [get-in-touch-overlay
           :job (<sub [::subs/get-in-touch-overlay-job-data])
           :candidate-name (:name header-data)
           :candidate-email (:email private-data)
           :on-ok #(dispatch [::events/hide-get-in-touch-overlay])])
        (when (<sub [::subs/show-job-selection-overlay?])
          (let [data (<sub [::subs/job-selection-overlay-data])]
            [job-selection-overlay
             :state (<sub [::subs/job-selection-overlay-state])
             :jobs data
             :selected-jobs (<sub [::subs/job-selection-overlay-job-selections])
             :dispatch-on-check [::events/toggle-job-for-application-state]
             :on-close #(dispatch [::events/hide-job-selection-overlay true])
             :on-ok #(dispatch [::events/hide-job-selection-overlay])]))]))])
