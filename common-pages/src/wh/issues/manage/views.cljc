(ns wh.issues.manage.views
  (:require
    #?(:cljs [wh.components.forms.views :as form])
    [wh.components.common :refer [link img wrap-img]]
    [wh.components.icons :refer [icon]]
    [wh.components.issue :refer [issue->status]]
    [wh.components.pagination :as pagination]
    [wh.issues.manage.subs :as subs]
    [wh.issues.manage.events :as events]
    [wh.re-frame.events :refer [dispatch]]
    [wh.re-frame.subs :refer [<sub]]
    [wh.routes :as routes]
    [wh.util :as util]
    [clojure.string :as str]))


(defn syncing-repos-overlay []
  [:div.manage-issues-syncing-repo
   [:div.manage-issues-syncing-repo__spinner]
   [:p.manage-issues-syncing-repo__text "Syncing repositories with GitHub"]])

(defn syncing-issues-overlay []
  [:div.manage-issues-syncing-issues
   [:div.manage-issues-syncing-issues__spinner]
   [:p.manage-issues-syncing-issues__text "Syncing issues with GitHub"]])

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
  [:div.main
   [:h1 (str "Manage " (<sub [::subs/full-repo-name]) " Issues")]
   (if (<sub [::subs/syncing-issues?])
     [syncing-issues-overlay]
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