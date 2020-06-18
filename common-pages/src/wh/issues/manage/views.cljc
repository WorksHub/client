(ns wh.issues.manage.views
  (:require
    #?(:cljs [wh.components.forms.views :as form])
    #?(:cljs [wh.components.github :as github])
    [clojure.string :as str]
    [wh.common.data :as data]
    [wh.common.time :as time]
    [wh.components.common :refer [link img wrap-img]]
    [wh.components.faq :as faq]
    [wh.components.icons :refer [icon]]
    [wh.components.issue :refer [issue->status]]
    [wh.components.pagination :as pagination]
    [wh.issues.manage.events :as events]
    [wh.issues.manage.subs :as subs]
    [wh.re-frame.events :refer [dispatch]]
    [wh.re-frame.subs :refer [<sub]]
    [wh.routes :as routes]
    [wh.util :as util]))


(defn syncing-repos-overlay []
  [:div.manage-issues-syncing-repo
   [:div.manage-issues-syncing-repo__spinner]
   [:p.manage-issues-syncing-repo__text "Syncing repositories with GitHub"]])

(defn sync-progress
  []
  (let [repo-sync (<sub [::subs/current-repo-sync])
        amount (if repo-sync (int (* 100 (/ (:running-issue-count repo-sync)
                                            (:total-issue-count repo-sync))))
                   0)
        amount-pc (str amount "%")]
    [:div.manage-issues__sync-progress
     [:div.progress-bar
      [:div.progress-bar__background.animated]
      [:div.progress-bar__foreground {:style {:width amount-pc}}]]
     [:span (when (:total-issue-count repo-sync)
              (str (or (:running-issue-count repo-sync) 0) "/" (:total-issue-count repo-sync)))]]))

(defn syncing-issues-overlay []
  [:div.manage-issues-syncing-issues
   [:div.is-flex
    [:p.manage-issues-syncing-issues__text "Syncing issues with GitHub"]
    [sync-progress]]
   (when (<sub [::subs/sync-wants-restart?])
     [:div.manage-issues-syncing-issues__restart
      [:span "Synchronisation appears to be taking longer than usual"]
      [:button.button.button--inverted
       {:on-click #(dispatch [::events/sync-repo-issues (<sub [::subs/repo]) {:force? true}])}
       "Restart"]])])

(defn repo-card
  [{:keys [owner owner-avatar description primary-language open-issues-count has-unpublished-issues] :as repo}]
  [:div.repository-card
   [:div.is-flex
    [:div.logo (if-let [url owner-avatar]
                 (wrap-img img url {})
                 [:div.empty-logo])]
    [:div.info
     [:div.title [link (str owner "/" (:name repo)) :manage-repository-issues :repo-name (:name repo) :owner owner :class "a--hover-red"]]]]
   [:div.description description]
   [:div.details
    [:div.repo-props
     [:ul.tags
      (when-let [language primary-language]
        [:li.tag language])]
     [:div.open-issues [icon "gh-issues" :tooltip "Open Issues"] open-issues-count]]
    ;; TODO add 'publish all' button to repos page
    (if (pos? open-issues-count)
      [:div.repository-card__buttons
       [link "Manage" :manage-repository-issues :repo-name (:name repo) :owner owner
        :class "button button--inverted btn__manage-issues"]
       [:a
        #?(:cljs
           {:on-click #(when (js/confirm "Please confirm you wish to publish all open issues?")
                         (set! js/window.location
                               (routes/path :manage-repository-issues
                                            :params {:repo-name (:name repo) :owner owner}
                                            :query-params {:publish-all true})))})
        [:button.button
         {:disabled (not has-unpublished-issues)}
         "Publish All"]]]
      [:div.repository-card__buttons
       [:button.button.button--disabled
        {:disabled true}
        "Manage"]
       [:button.button.button--disabled
        {:disabled true}
        "Publish All"]])]])

(defn repo-list []
  #?(:cljs
     [:div
      (doall
        (for [repo (<sub [::subs/repos])]
          [:div {:key (:name repo)}
           [repo-card repo]]))]))

(defn connect-gh-app-error []
  #?(:cljs
     [:div.manage-issues-syncing-repo
      [:p "Failed to connect WorksHub Github App, please try again"]
      [github/install-github-app {}]]))

(defn control-panel
  []
  #?(:cljs
     [:section.split-content-section.manage-issues__control-panel
      [:div.manage-issues__control-panel__cta
       [:p "Can't see all your repositories?"]
       [github/install-github-app {:label "Add/Remove repos"
                                   :class "button--large"}]]
      (when (false? (<sub [::subs/slack-connected?]))
        [:div.manage-issues__control-panel__cta.manage-issues__control-panel__cta--left
         [:p "Would you like to get notifications?"]
         [:a {:href (when-not (<sub [:user/admin?])
                      (routes/path :oauth-slack))}
          [:button.button.company-edit__integration
           {:id "manage-issues__integration--slack"}
           [:img {:src "/images/company/slack.svg"}]]]])
      [:div.manage-issues__control-panel__cta
       [:p "If you have any problems or questions along the way, please drop us an email"]
       [:a {:href "mailto:hello@works-hub.com"
            :target "_blank"
            :rel "noopener"}
        [:button.button.button--medium.button--inverted.button--lowercase.button--public
         "hello@works-hub.com"]]]]))

(defn issues-faqs
  []
  [:section.split-content-section.manage-issues__faqs
   [:h2 "FAQs"]
   [faq/faq-mini (get data/how-it-works-questions :company)]])

(defn page []
  #?(:cljs
     [:div.main.split-content
      [:div.split-content__main
       [:h1 "Connected Repositories"]
       [:div.spread-or-stack
        (cond
          (<sub [::subs/syncing-repos?])
          [:h3 ""]

          (pos? (<sub [::subs/open-issues-on-all-repos]))
          [:h3.manage-issues__subheading "You have " [:span.bold-text (<sub [::subs/number-of-published-issues])]
           " issues published on WorksHub."
           (when (= 0 (<sub [::subs/number-of-published-issues]))
             " Please select a repository and choose which issues to publish")]

          :else
          [:h3.manage-issues__subheading "There are no issues on selected repositories.
               Please add other repositories with open issues so you can publish them on WorksHub."])]
       (cond
         (<sub [::subs/connect-github-app-error?])
         [connect-gh-app-error]
         (<sub [::subs/syncing-repos?])
         [syncing-repos-overlay]
         :else
         [repo-list])
       "All done here?"
       [:div.manage-issues-buttons
        [:a.button.button.button--inverted.back
         {:href (routes/path :company-issues)}
         [icon "arrow-left"] "Back to Company Issues"]]]
      [:div.split-content__side
       [control-panel]
       [issues-faqs]]]))

(defn issues-list []
  #?(:cljs
     [:div.manage-issues-list
      (let [issues (<sub [::subs/issues])
            sync   (<sub [::subs/current-repo-sync])]
        [:div.repo__content
         [:div.repo__header
          [:div.manage-issues__sync-info
           (when-let [t (:time-finished sync)]
             [:p "Last GitHub sync: " (time/human-time (time/str->time t :date-time))])
           [:button.button.button--inverted
            {:on-click #(dispatch [::events/sync-repo-issues (<sub [::subs/repo]) {:force? true}])}
            "Re-sync now"]]
          [:div
           [:div.publish-container
            [:span.publish.publish-all "Publish All"]]
           [:div.repo__checkbox__container
            [:div.repo__checkbox
             [form/labelled-checkbox
              (and (seq issues)
                   (every? true? (map :published issues)))
              {:indeterminate? (> (count (distinct (map :published issues))) 1)
               :on-change      [::events/update-pending issues]}]]]
           [:div.publish-container
            [:span.publish "Published"]]]]
         [:div.issues
          (if (empty? issues)
            [:p "There are no open issues in this repository."]
            [:ul
             (for [issue issues]
               (let [derived-status (issue->status issue)]
                 [:li.issue.is-flex {:key (:id issue)}
                  [:p.manage-issues-list__issue__title
                   [:span {:class (util/merge-classes "issue-status" (str "issue-status--" derived-status))} [icon "issue-status"] [:span (str/capitalize derived-status)]]
                   [:span.issue-list__title (:title issue)]]
                  [form/labelled-checkbox (:published issue)
                   {:on-change [::events/update-pending [issue]]
                    :class     "issue-checkbox"}]]))])]])]))

(defn issues-page []
  [:div.main.manage-issues
   [:h1 (str "Manage " (<sub [::subs/full-repo-name]) " Issues")]
   (cond (<sub [::subs/syncing-issues?])
         [syncing-issues-overlay]
         (<sub [::subs/issues-loading?])
         [:div.manage-issues__queries-loading [:div.is-loading-spinner]]
         :else
         [issues-list])
   [:div.manage-issues-buttons
    [:a.button.button.button--inverted.back
     {:href (routes/path :manage-issues)}
     [icon "arrow-left"] "Back to All Repositories"]
    [:button.button.update {:disabled (<sub [::subs/update-disabled?])
                            :on-click #(when-not (<sub [::subs/update-disabled?])
                                         (dispatch [::events/save-changes]))}

     "Save"]]
   [pagination/pagination
    (<sub [::subs/current-page-number])
    (<sub [::subs/pagination])
    (<sub [:wh.subs/page])
    (<sub [:wh.subs/query-params])
    (<sub [:wh/page-params])]])
