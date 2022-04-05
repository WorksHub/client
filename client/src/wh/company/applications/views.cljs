(ns wh.company.applications.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch dispatch-sync]]
            [wh.common.data :refer [get-manager-name]]
            [wh.company.applications.db :as db]
            [wh.company.applications.events :as events]
            [wh.company.applications.subs :as subs]
            [wh.components.cards :refer [match-circle]]
            [wh.components.common :refer [link link-user]]
            [wh.components.conversation-link.core :as conversation-link]
            [wh.components.forms.views :as forms]
            [wh.components.icons :refer [icon]]
            [wh.components.modal :as modal]
            [wh.components.overlay.views :refer [popup-wrapper]]
            [wh.components.tabs.views :refer [tabs]]
            [wh.routes :as routes]
            [wh.subs :refer [<sub]]))

(defn infobox-row [icon-name content]
  [:div.company-application__infobox-row
   [:div.company-application__infobox-icon
    [icon icon-name :class icon-name]]
   [:div.company-application__infobox-content
    content]])

(defn match [x]
  [:div.company-application__match
   [match-circle {:score x}]
   (int (* x 100))
   "% match"])

(defn job-stat [icon-name count & [hover-text]]
  [:div.company-job__stat
   (cond-> {:class (str "company-job__" icon-name)}
     hover-text (assoc :title hover-text))
   [icon icon-name]
   [:div.company-job__stat-number count]])

(defn job-header
  [{:keys [job title-link]}]
  (let [skeleton?                                                 (not job)
        logo                                                      (<sub [::subs/logo])
        {:keys [id slug title stats display-location first-published]} job]
    [:div.company-application__job
     {:class (when skeleton? "skeleton")}
     (cond
       skeleton? [:div.company-application__job-logo]
       logo      [:img.company-application__job-logo {:src logo}])
     [:div.company-application__job-title (or title-link [link title :job
                                                          :slug slug
                                                          :class "a--hover-red"
                                                          :on-click #(dispatch-sync [:wh.job/preset-job-data job])])]
     [:div.company-application__job-location display-location]
     [:div.company-application__job-posted (when-not skeleton? "Posted on ") first-published]
     (if skeleton?
       [:div.company-application__job-stats
        [job-stat "circle"]
        [job-stat "circle"]
        [job-stat "circle"]]
       [:div.company-application__job-stats
        [job-stat "views" (:views stats)]
        [job-stat "like" (:likes stats)]
        [job-stat "applications" (:applications stats)]])]))

(defn set-application-state!
  [job-id user-id state]
  (if job-id
    #(dispatch [::events/set-application-state job-id user-id (name state)])
    #(dispatch [::events/show-job-selection-overlay user-id (name state)])))

(defn application-buttons
  [& {:keys [class state on-get-in-touch on-pass on-hire could-hire?]}]
  (if (= state "hired")
    [:div [:button.button {:on-click on-get-in-touch} "Move back to Interviewing"]]
    [:div
     {:class class}
     (when (and (not= state "get_in_touch")
                could-hire?)
       [:button.button {:on-click on-hire} "Hire"])
     (if (= state "get_in_touch")
       [:button.button {:on-click on-hire} "Hire"]
       [:button.button {:on-click on-get-in-touch} "Get in touch"])
     (when-not (or (= state "rejected")
                   (= state "pass"))
       [:button.button.button--inverted {:on-click on-pass} "Pass"])]))

(defn admin-buttons
  [& {:keys [class state on-approve on-reject on-get-in-touch on-pass on-hire]}]
  [:div
   {:class class}
   (case state
     "pending"
     [:<> [:button.button {:on-click on-approve :data-test "approve-application"}
           "Approve"]
      [:button.button.button--inverted {:on-click on-reject} "Reject"]]
     "approved"
     [:<> [:button.button {:on-click on-get-in-touch} "Get in touch"]
      [:button.button.button--inverted {:on-click on-pass} "Pass"]]
     "pass"
     [:<> [:button.button {:on-click on-get-in-touch} "Get in touch"]]
     "get_in_touch"
     [:<> [:button.button {:on-click on-hire} "Hire"]
      [:button.button.button--inverted {:on-click on-pass} "Pass"]]
     "rejected"
     [:<> [:button.button {:on-click on-approve} "Approve"]]
     "hired"
     [:<> [:button.button {:on-click on-get-in-touch} "Move back to Interviewing"]]
     nil)])

(defn format-job-title [{{name :name} :company title :title}]
  [:div.company-application__job-details
   [:div.company-name name] [:div (str \u2002 "- " title)]])

(defn application-card
  [{:keys [user-id conversation timestamp display-location other-urls email skills score cv cover-letter state note job cv-downloadable? cover-letter-downloadable?]
    :as   app} job-id]
  (let [admin?    (<sub [:user/admin?])
        skeleton? (empty? app)
        job-id    (or job-id (:job-id app))]
    [:div.card.company-application
     {:class (when skeleton? "skeleton")}
     [:div.company-application__name
      (when-not skeleton?
        [link-user (:name app) (<sub [:user/admin?]) :id user-id :class "a--hover-red" :query-params {:job-id job-id}])]
     [:div.company-application__applied-on-section
      [:div.company-application__applied-on (when-not skeleton? "Applied on ") timestamp]
      [:div.company-application__view-documents
       [:div.company-application__view-document
        (if (or cv skeleton?)
          [:a.a--underlined.company-application__view-cv
           {:href      cv
            :target    "_blank"
            :data-test (when cv "applicant-cv-link")
            :rel       "noopener"
            :download  cv-downloadable?}
           (when-not skeleton? "View CV")]
          [:i "No CV provided"])]
       [:div.company-application__view-document
        (when-not skeleton?
          [link-user "View Profile" (<sub [:user/admin?]) :id user-id :class "a--underlined" :query-params {:job-id job-id}])]
       [:div.company-application__view-document
        (if (or cover-letter skeleton?)
          [:a.a--underlined.company-application__view-cover-letter {:href     cover-letter
                                                                    :target   "_blank"
                                                                    :rel      "noopener"
                                                                    :download cover-letter-downloadable?}
           (when-not skeleton? "View Cover Letter")]
          [:i "No Cover Letter provided"])]]
      (when-not skeleton?
        [conversation-link/link
         {:conversations-enabled? (<sub [:wh/conversations-enabled?])}
         conversation])
      (when (and job admin?)
        (format-job-title job))]
     (if skeleton?
       (into [:div.company-application__infobox]
             (repeat 2 [:div.company-application__infobox-row [icon "circle"]]))
       [:div.company-application__infobox {:class (when (and job admin?) "company-application__infobox--short")}
        (when-not (and job admin?)
          (infobox-row "email"
                       (when email
                         [:a {:href (str "mailto:" email)} email])))
        (infobox-row "job-location"
                     (or display-location [:i "No location preference"]))
        (when (seq other-urls)
          (into
            [:div.company-application__infobox-row]
            (for [{:keys [type url]} other-urls]
              [:div.company-application__infobox-icon
               [:a {:href url, :target "_blank", :rel "noopener"}
                [icon (name type) :class (name type)]]])))])
     (cond score     (match score)
           skeleton? [:div.company-application__match [icon "circle"] [:span ""]]
           :else     [:div.company-application__match])
     (into
       [:ul.company-application__tags.tags]
       (map (fn [{:keys [name highlighted]}]
              [(if (or (not job-id) highlighted) :li.tag.tag--selected :li.tag.tag--unselected) name])
            (if skeleton?
              (repeatedly 6 #(hash-map :name (apply str (repeat (+ 8 (rand-int 30)) " ")) :highlighted true)) skills)))
     (when-not skeleton?
       (if admin?
         [:div.company-application__notes.is-flex
          {:class (when (str/blank? note) "company-application__notes--empty")}
          [icon "edit"]
          [:div
           {:class    (when job-id "clickable")
            :on-click (when job-id #(dispatch [::events/show-notes-overlay user-id job-id]))}
           (if (str/blank? note)
             [:div (if job-id "Add applicant notes" "Select a job to add a note")]
             [:div {:class "company-application__notes__note"} note])]]
         [:div.company-application__notes.is-flex
          {:class (when (str/blank? note) "company-application__notes--empty")}
          [:div {:class "company-application__notes__note"} note]]))
     [:div.company-application__cta-buttons
      (when-not skeleton?
        (if admin?
          [admin-buttons
           :class "company-application__admin-buttons"
           :state state
           :on-approve (set-application-state! job-id user-id :approve)
           :on-reject  (set-application-state! job-id user-id :reject)
           :on-get-in-touch (set-application-state! job-id user-id :get_in_touch)
           :on-pass (set-application-state! job-id user-id :pass)
           :on-hire (set-application-state! job-id user-id :hire)]
          [application-buttons
           :class "company-application__application-buttons"
           :state state
           :on-get-in-touch (set-application-state! job-id user-id :get_in_touch)
           :on-pass (set-application-state! job-id user-id :pass)
           :on-hire (set-application-state! job-id user-id :hire)]))]]))

;;

(defn- candidate-email-link
  [{:keys [job candidate-email] :as _opts}]
  [:p
   [:a.a--underlined
    {:href (str "mailto:" candidate-email "?subject=Requesting interview regarding "
                (if (:title job)
                  (str (:title job) " role")
                  "multiple roles"))}
    candidate-email]])

(defn- message-for-take-off-clients
  [{:keys [job candidate-name] :as _opts}]
  (let [{:keys [manager]} (<sub [:wh.user.subs/company])
        manager-name (when manager (get-manager-name manager))]
    (if manager
      [:span "Your manager "
       [:a.a--underlined
        {:href (str "mailto:" manager "?subject=Requesting interview with " candidate-name " regarding "
                    (if (:title job)
                      (str (:title job) " role")
                      "multiple roles"))}
        manager-name]
       " will be in touch about this candidate \uD83D\uDCAB"]
      "Your manager will be in touch about this candidate \uD83D\uDCAB")))

(defn- message-for-general-clients
  [{:keys [candidate-name] :as _opts}]
  [:span [:span "This is " [:strong (str candidate-name "'s")] " email address."]
   [:br]
   [:span "Please introduce yourself \uD83E\uDD1D"]])

(defn- overlay-no-conversations
  "shown if conversation feature is not enabled"
  [{:keys [on-ok] :as opts}]
  (let [{:keys [package]} (<sub [:wh.user.subs/company])]
    [popup-wrapper
     {:id    :get-in-touch
      :on-ok on-ok
      :class "company-applications"}
     [:h1 "Great!"]
     [:p (if (= "take_off" package)
           [message-for-take-off-clients opts]
           [:<>
            [message-for-general-clients opts]
            [:br] [:br]
            [candidate-email-link opts]])]]))

(defn- overlay-with-conversations [{:keys [on-ok candidate-email conversation-id] :as opts}]
  (let [{:keys [package]} (<sub [:wh.user.subs/company])]
    [modal/modal {:open? true
                  :on-request-close on-ok
                  :label "Get in touch"}
     [modal/header {:title "Get in touch"
                    :on-close on-ok}]
     (if (= "take_off" package)
       [:<>
        [modal/body
         [message-for-take-off-clients opts]]
        [modal/footer
         [modal/button {:text     "Ok"
                        :on-click on-ok}]]]
       [:<>
        [modal/body
         [:div
          [:p "You can message a candidate directly using " [:b "conversations"] " on our platform."]
          [:p "Or write to a candidate:"
           [:br]
           [:a {:href (str "mailto:" candidate-email)
                :style {:text-decoration "underline"}} candidate-email]]]]
        [modal/footer
         [modal/button {:text     "Use email"
                        :type     :secondary
                        :on-click on-ok}]
         [modal/button {:text     "Open conversation"
                        :href     (if conversation-id
                                    (routes/path :conversation :params {:id conversation-id})
                                    (routes/path :conversations))}]]])]))

(defn get-in-touch-overlay [& opts]
  (if (<sub [:wh/conversations-enabled?])
    [overlay-with-conversations opts]
    [overlay-no-conversations opts]))

;;

(defn state->verb
  [s]
  (case s
    "pass"         "pass on"
    "get_in_touch" "discuss"
    "hire"         "hire for"
    s))

(defn job-selection-overlay
  [& {:keys [jobs state on-ok on-close dispatch-on-check selected-jobs]}]
  [popup-wrapper
   {:id :job-selection
    :on-close on-close
    :on-ok on-ok
    :show-close? true
    :button-label (str/replace state #"\_" " ")
    :button-class (when (or (= state "pass") (= state "reject")) "button--inverted")
    :class "company-applications"}
   [:p (str "Which roles would you like to " (state->verb state) "?")]
   [:div.job-selection__jobs
    (doall
     (for [{:keys [id company-name title]} jobs]
       ^{:key id}
       [:div.job-selection__job
        [:span.job-selection__job__title (str company-name " - " title)]
        [:div.job-selection__job__checkbox
         [forms/labelled-checkbox (contains? selected-jobs id) {:on-change (conj dispatch-on-check id)}]]]))]])

(defn notes-overlay
  [& {:keys [user-id value on-ok on-close on-change]}]
  [popup-wrapper
   {:id :notes
    :on-ok on-ok
    :on-close on-close
    :button-label "Add Note"}
   [:form.wh-formx.wh-formx__layout
    [forms/text-field
     value
     {:type :textarea
      :label [:span "Notes on this applicant that are relevant to this role"]
      :on-change on-change}]]])

(defn page-header
  [admin-dashboard? job-id]
  (let [select-job-link (<sub [::subs/select-job-link])
        admin?    (<sub [:user/admin?])]
    [:section.company-applications__header
     [:div.wh-formx.company-applications__title
      [:h1 "Applications"]
      (if job-id
        [:a
         {:href (if-let [company-id (get-in select-job-link [:options :id])]
                  (routes/path (:handler select-job-link) :params {:id (get-in select-job-link [:options :id])})
                  (routes/path (:handler select-job-link)))}
         [:button.button.button--inverted.is-full-width-mobile
          [icon "arrow-left" ] "Back to All "
          (when admin?
            [:span.company-applications__title__company-name
             (<sub [::subs/company-name])])]]
        (let [dropdown-content   (<sub (if admin-dashboard? [::subs/companies-dropdown] [::subs/jobs-dropdown]))
              dropdown-selection (if admin-dashboard? [::events/select-company] [::events/select-job])]
          [forms/select-input nil
           {:options   (or dropdown-content [])
            :on-change dropdown-selection
            :disabled  (nil? dropdown-content)}]))]
     (when job-id
       [job-header {:job (<sub [::subs/current-job])}])]))

(defn generate-skeleton-apps
  [n]
  (map (fn [x] {:user-id x :job-id x}) (range n)))

(defn applications-list
  [job-id apps-loading?]
  (let [applications  (<sub [::subs/applications])
        skeleton?     (nil? applications)
        current-tab   (<sub [::subs/current-tab])
        tab-frequency (<sub [::subs/tab-frequency current-tab])
        tab-list      (<sub [::subs/tabs])]
    [:section.company-applications__applications-list
     (if (not tab-list)
       [:div.company-applications__tabs-spacer]
       [:div.company-applications__tabs
        [tabs
         current-tab
         tab-list
         #(dispatch [::events/select-tab %])]])
     (if (and (coll? applications) (empty? applications))
       [:h3 "We have no applications matching your criteria."]
       (concat (for [part (partition-all 3 (if skeleton?
                                             (generate-skeleton-apps (max 3 (min db/apps-page-size tab-frequency)))
                                             applications))]
                 (into
                  [:div.columns {:key (map (comp str (juxt :user-id :job-id)) part)}]
                  (for [card part]
                    [:div.column.is-4 [application-card (when-not skeleton? card) job-id]])))
               [(cond
                  apps-loading?
                  [:div.company-applications__loader
                   {:key "loader-applications"}]
                  (<sub [::subs/show-load-more-applications?])
                  [:div.company-applications__load-more.has-text-centered
                   {:key "load-more-latest-applications"}
                   [:button.button.button--medium.button--inverted
                    {:on-click #(dispatch [::events/fetch-more-applications])}
                    "Load More"]])]))]))

(defn applications-summary
  [apps-loading?]
  (let [jobs (<sub [::subs/latest-applied-jobs])
        applications (<sub [::subs/applications-by-jobs])
        select-job-link (<sub [::subs/select-job-link])
        skeleton-jobs? (nil? jobs)]
    [:section.company-applications__application-summaries
     (if (and (coll? jobs) (empty? jobs))
       [:h3 "There are currently no applications for your jobs."]
       (concat (for [job (or jobs (map (fn [x] {:id x}) (range 3)))]
                 (let [qp {:job-id (:id job)}
                       {:keys [applications current-tab frequencies current-frequency loading?]} (get applications (:id job))]
                   [:div.company-applications__application-summary
                    {:key (:id job)}
                    [job-header {:job (when-not skeleton-jobs? job)
                                 :title-link [link (-> select-job-link
                                                       (assoc :text (:title job))
                                                       (assoc-in [:options :query-params] qp)
                                                       (assoc-in [:options :class] "a--hover-red")
                                                       (assoc-in [:options :on-click] #(dispatch-sync [:wh.job/preset-job-data job])))]}]

                    (if (not frequencies)
                      [:div.company-applications__tabs-spacer]
                      [:div.company-applications__tabs
                       [tabs
                        current-tab
                        frequencies
                        #(dispatch (let [qp (assoc qp :tab (name %))]
                                     (if-let [company-id (get-in select-job-link [:options :id])]
                                       [:wh.events/nav (:handler select-job-link) :params {:id company-id} :query-params qp]
                                       [:wh.events/nav (:handler select-job-link) :query-params qp])))]])
                    (into
                     [:div.columns]
                     (if (and (coll? applications) (empty? applications))
                       [[:h3 "We have no applications matching your criteria."]]
                       (for [application (or applications (repeat 3 {}))]
                         [:div.column.is-4 [application-card application (:id job)]])))
                    (when (< (count applications) current-frequency)
                      [:div.company-applications__view-applicants.has-text-centered
                       [:a
                        {:href (if-let [company-id (get-in select-job-link [:options :id])]
                                 (routes/path (:handler select-job-link) :params {:id (get-in select-job-link [:options :id])} :query-params qp)
                                 (routes/path (:handler select-job-link) :query-params qp))}
                        [:button.button.button--inverted.is-full-width-mobile
                         "View All Applicants" [icon "arrow-right"]]]])
                    (when (and applications loading?)
                      [:div.company-applications__application-summary__loading
                       [:div.company-applications__loader]])]))
               [(cond
                  apps-loading?
                  [:div.company-applications__loader
                   {:key "loader-latest-applied-jobs"}]
                  (<sub [::subs/show-load-more-latest-applied-jobs?])
                  [:div.company-applications__load-more.has-text-centered
                   {:key "load-more-latest-applied-jobs"}
                   [:button.button.button--medium.button--inverted
                    {:on-click #(dispatch [::events/fetch-more-latest-applied-jobs])}
                    "Load More"]])]))]))

(defn page []
  (let [job-id           (<sub [::subs/job-id])
        apps-loading?    (<sub [::subs/applications-loading?])
        admin-dashboard? (<sub [::subs/admin-dashboard?])
        has-permission?  (<sub [::subs/has-permission-to-view-applications?])]
    (when has-permission?
      [:div.main.company-applications
       [page-header admin-dashboard? job-id]
       (if (or admin-dashboard? job-id)
         [applications-list job-id apps-loading?]
         [applications-summary apps-loading?])
       ;; overlays
       (when (<sub [::subs/show-get-in-touch-overlay?])
         (let [data (<sub [::subs/get-in-touch-overlay-data])]
           [get-in-touch-overlay
            :job (:job data)
            :conversation-id (:conversation-id data)
            :candidate-name (get-in data [:user :name])
            :candidate-email (get-in data [:user :email])
            :on-ok #(dispatch [::events/hide-get-in-touch-overlay])]))
       (when (<sub [::subs/show-job-selection-overlay?])
         (let [data (<sub [::subs/job-selection-overlay-data])]
           [job-selection-overlay
            :state (<sub [::subs/job-selection-overlay-state])
            :jobs data
            :selected-jobs (<sub [::subs/job-selection-overlay-job-selections])
            :dispatch-on-check [::events/toggle-job-for-application-state]
            :on-close #(dispatch [::events/hide-job-selection-overlay true])
            :on-ok #(dispatch [::events/hide-job-selection-overlay])]))
       (when (<sub [::subs/show-notes-overlay?])
         (let [[user-id job-id note] (<sub [::subs/notes-overlay-data])]
           [notes-overlay
            :value note
            :on-change [::events/edit-note user-id job-id]
            :on-close #(dispatch [::events/hide-notes-overlay true])
            :on-ok #(dispatch [::events/hide-notes-overlay])]))])))
