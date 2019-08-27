(ns wh.issues.manage.views
  (:require
    #?(:cljs [wh.components.forms.views :as form])
    [clojure.string :as str]
    [wh.components.common :refer [link img wrap-img]]
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
      [:div.progress-bar__background]
      [:div.progress-bar__foreground {:style {:width amount-pc}}]]
     [:span (or (:running-issue-count repo-sync) 0) "/" (or (:total-issue-count repo-sync) "???")]]))

(defn syncing-issues-overlay []
  [:div.manage-issues-syncing-issues
   [:p.manage-issues-syncing-issues__text "Syncing issues with GitHub"]
   [sync-progress]])

(defn repo-card
  [org {:keys [owner description primary-language] :as repo}]
  [:div.repository-card
   [:div.is-flex
    [:div.logo (if-let [url (:avatar-url org)]
                 (wrap-img img url {})
                 [:div.empty-logo])]
    [:div.info
     [:div.title [link (str owner "/" (:name repo)) :manage-repository-issues :repo-name (:name repo) :owner owner :class "a--hover-red"]]]]
   [:div.description description]
   [:div.details
    [:ul.tags
     (when-let [language primary-language]
       [:li.tag language])]
    [link "Manage Issues" :manage-repository-issues :repo-name (:name repo) :owner owner :class "button button--inverted btn__manage-issues"]]])

(defn repo-list []
  #?(:cljs
     [:div
      (doall
        (for [org (<sub [::subs/orgs])
              repo (:repositories org)]
          [:div {:key (:name repo)}
           [repo-card org repo]]))]))

(defn page []
  [:div.main
   [:h1 "GitHub Repositories"]
   [:h3 "Select a repository to manage individual issues"]
   (if (<sub [::subs/syncing-repos?])
     [syncing-repos-overlay]
     [repo-list])
   [:div.manage-issues-buttons
    [:a.button.button.button--inverted.back
     {:href (routes/path :company-issues)}
     [icon "arrow-left"] "Back to Company Issues"]]])

(defn issues-list []
  #?(:cljs
     [:div.manage-issues-list
      (let [issues (<sub [::subs/issues])]
        [:div.repo__content
         [:div.publish-container
          [:span.publish.publish-all "Publish All"]]
         [:div.repo__checkbox__container
          [:div.repo__checkbox
           [form/labelled-checkbox
            (and (seq issues)
                 (every? true? (map :published issues)))
            {:indeterminate? (> (count (distinct (map :published issues))) 1)
             :on-change [::events/update-pending issues]}]]]
         [:div.publish-container
          [:span.publish "Published"]]
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
    (<sub [:wh.subs/page-params])]])
