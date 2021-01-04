(ns wh.profile.section-company
  (:require [wh.components.common :refer [link]]
            [wh.components.modal :as modal]
            [wh.logged-in.profile.components :as components]
            [wh.profile.db :as profile]
            [wh.styles.profile :as styles]
            [wh.util :as util]))

(def btn-cls (util/mc styles/button styles/button--small))
(def btn-cls-inverted (util/mc styles/button styles/button--small styles/button--inverted))

(defn btn-get-in-touch [{:keys [on-get-in-touch updating-state?] :as opts}]
  [:button {:on-click on-get-in-touch
            :class btn-cls
            :disabled updating-state?}
   "Get in touch"])

(defn btn-hire [{:keys [on-hire updating-state?]}]
  [:button {:on-click on-hire
            :class btn-cls
            :disabled updating-state?}
   "Hire"])

(defn btn-pass [{:keys [on-pass updating-state?]}]
  [:button {:on-click on-pass
            :class btn-cls-inverted
            :disabled updating-state?}
   "Pass"])

(defn buttons [& children]
  (into [:div {:class styles/job-application__controls}] children))

(defn control-buttons [{:keys [current-application] :as opts}]
  (let [state (:state current-application)]
    (cond (#{(profile/application-state :pending)
             (profile/application-state :approved)} state)
          [buttons [btn-pass opts] [btn-get-in-touch opts]]
          (#{(profile/application-state :pass)
             (profile/application-state :rejected)} state)
          [buttons [btn-get-in-touch opts]]
          (= (profile/application-state :get-in-touch) state)
          [buttons [btn-pass opts] [btn-hire opts]]
          :else
          nil)))

(defn manager-note [note]
  (when note
    [:div {:class styles/job-application__note}
     [:div {:class styles/job-application__note-title} "Manager note:"]
     [:div note]]))

(defn job-application [type application {:keys [user] :as opts}]
  (let [current?           (= type :current)
        {:keys [job note]} application
        cover-letter-url   (get-in application [:cover-letter :file :url])
        cv-url             (get-in user [:cv :file :url])]
    [:div {:class styles/job-application__wrapper}
     [:div {:class styles/job-application}
      [components/job-link job]
      [components/application-state application user type]
      (when current?
        [:<>
         (when cover-letter-url
           [components/underline-link {:href     cover-letter-url
                                       :text     "View cover letter"
                                       :new-tab? true}])
         (if cv-url
           [components/underline-link {:href     cv-url
                                       :text     "View CV"
                                       :new-tab? true}]
           [:div {:class styles/job-application__missing}
            "No CV provided."])
         [manager-note note]])]
     (when current? [control-buttons opts])]))

(defn modal-user-info [{:keys [user on-modal-close modal-opened?]}]
  [modal/modal {:open? modal-opened?
                :on-request-close #()
                :label "Get in touch"}
   [modal/header {:title "Get in touch"
                  :on-close on-modal-close}]
   [modal/body
    [:div "This is " (:name user) "'s email address."]
    [components/underline-link {:href (str "mailto:" (:email user))
                                :text (:email user)}]
    [:div "Please introduce yourself."]]
   [modal/footer
    [modal/button {:text "Ok"
                   :on-click on-modal-close}]]])

(defn controls [{:keys [current-application other-applications] :as opts}]
  [:<>
   [modal-user-info opts]
   [components/section-custom {:type :company}
    [components/sec-title "Applications"]
    [:div {:class styles/subsection__wrapper}
      ;;
      (when current-application
        [components/subsection "Current application:"
         [job-application :current current-application opts]])
      ;;
      (let [subsection-title (if current-application
                               "Other applications:"
                               "All applications:")]
        (when (seq other-applications)
          [components/subsection
           subsection-title
           [components/job-list
            (for [application other-applications]
              ^{:key (:timestamp application)}
              [:li [job-application :other application opts]])]]))]]])
