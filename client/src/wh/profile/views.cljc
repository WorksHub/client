(ns wh.profile.views
  (:require
    #?(:cljs [wh.profile.section-company :as section-company])
    [clojure.string :refer [ends-with?]]
    [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
    [wh.components.loader :refer [loader-full-page]]
    [wh.components.not-found :as not-found]
    [wh.interop :as interop]
    [wh.logged-in.profile.components :as components]
    [wh.profile.db :as profile]
    [wh.profile.events :as profile-events]
    [wh.profile.subs :as subs]
    [wh.re-frame.subs :refer [<sub]]))

(defn section-for-company []
  #?(:cljs (let [user                  (<sub [::subs/profile])
                 company-view?         (<sub [:user/company?])
                 applications          (<sub [::subs/applications])
                 current-application   (<sub [::subs/current-application])
                 set-application-state #(dispatch [::profile-events/set-application-state {:user        user
                                                                                           :application current-application
                                                                                           :state       %}])]
             (when (and company-view? (seq applications))
               [section-company/controls
                {:current-application current-application
                 :user                user
                 :other-applications  (<sub [::subs/other-applications])
                 :updating-state?     (<sub [::subs/updating-application-state?])
                 :company             (<sub [::subs/company])
                 :on-get-in-touch     #(set-application-state (profile/application-action :get-in-touch))
                 :on-pass             #(set-application-state (profile/application-action :pass))
                 :on-hire             #(set-application-state (profile/application-action :hire))
                 :on-modal-close      #(dispatch [::profile-events/close-user-info-modal])
                 :modal-opened?       (<sub [::subs/user-info-modal-opened?])
                 :cv-visible?         (<sub [::subs/cv-visible?])}]))))

;I only managed to test that the .pdf is loading fine if I substitute cv-url
;with something line cv-url "http://www.africau.edu/images/default/sample.pdf" (or any valid .pdf link)
(defn section-cv []
  (let [company-view? (<sub [:user/company?])
        cv-visible?   (<sub [::subs/cv-visible?])
        user-cv       (get-in (<sub [::subs/profile]) [:cv :file])
        cv-url        (:url user-cv)
        cv-name       (:name user-cv)]
    (when (and company-view? cv-visible? cv-url cv-name (ends-with? cv-name ".pdf"))
      [components/section-cv cv-url])))

(defn section-stats []
  (let [articles (<sub [::subs/blogs])
        issues (<sub [::subs/issues])]
    [components/section-stats {:is-owner?      false
                               :percentile     (<sub [::subs/percentile])
                               :created        (<sub [::subs/created])
                               :articles-count (count articles)
                               :issues-count   (count issues)}]))

(defn section-skills []
  [components/section-skills {:type         :public
                              :skills       (<sub [::subs/skills])
                              :interests    (<sub [::subs/interests])
                              :query-params (<sub [:wh/query-params])
                              :max-skills   profile/maximum-skills}])

(defn section-contributions []
  (when (boolean (<sub [::subs/contributions-collection]))
    [components/section-contributions
     (<sub [::subs/contributions-calendar])
     (<sub [::subs/contributions-count])
     (<sub [::subs/contributions-repos])
     (<sub [::subs/contributions-months])]))

(defn section-articles []
  (let [articles (<sub [::subs/blogs])]
    [components/section-articles articles :public]))

(defn section-issues []
  (let [issues (<sub [::subs/issues])]
    [components/section-issues issues :public]))

(defn profile-column []
  [components/profile (<sub [::subs/profile])
   {:twitter       (<sub [::subs/social :twitter])
    :stackoverflow (<sub [::subs/social :stackoverflow])
    :github        (<sub [::subs/social :github])
    :last-seen     (<sub [::subs/last-seen])
    :updated       (<sub [::subs/updated])}
   :public])

(defn section-private-details []
  (let [default-fields #{:visa :preferred-locations :current-location}
        extended-fields (conj default-fields :email)]
    (fn []
      (let [user-details (<sub [::subs/user-details])
            contacted-or-hired? (<sub [::subs/was-contacted-or-hired?])
            admin-view? (<sub [::subs/admin-view?])
            company-view? (<sub [::subs/company-view?])]
        (when (or admin-view? company-view?)
          [components/section
           [components/edit-user-private-info
            :public
            (assoc user-details
              :title "Details"
              :fields (if contacted-or-hired? extended-fields default-fields))]])))))

(defn show-auth-popup-ssr []
  #?(:clj (when (and (not (<sub [:user/logged-in?]))
                  (<sub [::subs/show-auth-popup?]))
            [:script (interop/show-auth-popup :see-application :current-url)])))

(defn page []
  [:<>
   (cond
     (<sub [::subs/error?])
     [not-found/not-found-profile]
     ;;
     (<sub [::subs/loader?])
     [loader-full-page]
     ;;
     :else
     [components/container
      [profile-column]
      (if (<sub [::subs/hide-profile?])
        [components/profile-hidden-message]
        [components/content
         [section-for-company]
         [section-cv]
         [section-stats]
         [section-skills]
         [section-private-details]
         [section-contributions]
         [section-articles]
         [section-issues]])])
   [show-auth-popup-ssr]])
