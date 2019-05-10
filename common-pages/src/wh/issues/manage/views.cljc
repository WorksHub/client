(ns wh.issues.manage.views
  (:require
    #?(:cljs [wh.components.forms.views :as form])
    [wh.components.common :refer [link img wrap-img]]
    [wh.components.icons :refer [icon]]
    [wh.issues.manage.subs :as subs]
    [wh.issues.manage.events :as events]
    [wh.re-frame.events :refer [dispatch]]
    [wh.re-frame.subs :refer [<sub]]
    [wh.routes :as routes]
    [wh.util :as util]))


(defn syncing-repos-overlay []
  [:div.manage-issues-syncing-repo
   [:div.manage-issues-syncing-repo__spinner]
   [:p.manage-issues-syncing-repo__text "Syncing repositories with GitHub"]])

(defn syncing-issues-overlay []
  [:div.manage-issues-syncing-issues
   [:div.manage-issues-syncing-issues__spinner]
   [:p.manage-issues-syncing-issues__text "Syncing issues with GitHub"]])

(defn issues-list []
  #?(:cljs
     [:div.manage-issues-list
      (doall
        (for [org (<sub [::subs/orgs])
              repo (:repositories org)
              :let [open? (<sub [::subs/open-repo? repo])
                    syncing? (<sub [::subs/syncing-issues? repo])
                    issues (<sub [::subs/issues repo])]]
          [:div.repo {:key (:name repo)}
           [:div.repo__header
            [:div.repo__header__content.is-flex  {:on-click #(dispatch [::events/toggle-repo repo])}
             [:div
              [icon (if open? "minus" "plus") :class "repo__header__toggle"]]
             (if-let [avatar-url (:avatar-url org)]
               (wrap-img img avatar-url
                         {:alt (str (:name repo) " logo")
                          :w   30 :h 30 :class "repo__header__logo"})
               [:div.empty-logo])
             [:div.repo__header__title (str (:owner repo) "/" (:name repo))]]]
           (when open?
             (if syncing?
               [syncing-issues-overlay]
               [:div.repo__content
                [:div.repo__language
                 [:ul.tags [:li.tag (:primary-language repo)]]]
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
                      [:li.issue.is-flex {:key (:id issue)}
                       [:p.manage-issues-list__issue__title (:title issue)]
                       [form/labelled-checkbox (:published issue)
                        {:on-change [::events/update-pending [issue]]
                         :class     "issue-checkbox"}]])])]]))]))]))

(defn page []
  [:div.main
   [:h1 "Manage Issues"]
   [:h3 "Click on a repository to manage individual issues"]
   (if (<sub [::subs/syncing-repos?])
     [syncing-repos-overlay]
     [issues-list])
   [:div.manage-issues-buttons
    [:a.button.button.button--inverted.back
     {:href (routes/path :company-issues)}
     [icon "arrow-left"] "Back to All"]
    [:button.button.update {:disabled (<sub [::subs/update-disabled?])
                            :on-click #(when-not (<sub [::subs/update-disabled?])
                                         (dispatch [::events/save-changes]))}
     "Save"]]])
