(ns wh.admin.candidates.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch dispatch-sync]]
    [wh.admin.candidates.events :as events]
    [wh.admin.candidates.subs :as subs]
    [wh.components.common :refer [link]]
    [wh.components.forms.views :refer [text-field]]
    [wh.components.icons :refer [icon url-icons]]
    [wh.components.loader :refer [loader]]
    [wh.components.not-found :as not-found]
    [wh.components.pagination :as pagination]
    [wh.routes :refer [path]]
    [wh.subs :refer [<sub]]))

(defn approval->str [{:keys [source time status]}]
  (str (str/capitalize status) (when source (str " by " source)) (when time (str " on " time))))

(defn candidate-pod [{:keys [name email board skills preferred-locations cv-link cv updating
                             object-id hubspot-profile-url approval other-urls updated type company-id]}]
  (let [location (str/join ", " (-> preferred-locations first (select-keys [:city :country]) vals))]
    [:div.candidate-pod
     [:div.candidate-pod__image-section
      [icon board :class "candidate-pod__image-section__logo"]]
     [:div.columns.candidate-pod__data-section
      [:div.column.candidate-pod__first-section
       [url-icons other-urls "candidate-pod__other-urls"]
       [:p.candidate-pod__name
        [link (if (str/blank? name) "Profile" name) :candidate :id object-id]]
       [:p [:a.a--underlined {:href (str "mailto:" email)} email] (when-not (str/blank? location) (str " | " location) )]
       [:p.candidate-pod__cv-links
        (when hubspot-profile-url
          [:a.a--underlined {:href hubspot-profile-url
                             :target "_blank"
                             :rel "noopener"} "HubSpot"])]
       [:p.candidate-pod__cv-links
        (when (:url cv)
          [:a.a--underlined {:href (:url cv)
                             :target "_blank"
                             :rel "noopener"} "CV"])
        (when (:url cv-link)
          [:a.a--underlined {:href (:url cv-link)
                             :target "_blank"
                             :rel "noopener"} "CV Link"])]
       [:div
        [:button.button.button--green.button--small.candidate-pod__details__approve-btn
         {:disabled (= (:status approval) "approved")
          :title (approval->str approval)
          :class (when (= "approved" updating) "button--loading")
          :on-click #(do (.preventDefault %)
                         (dispatch [:candidates/set-approval-status object-id email "approved"]))} "Approve"]
        [:button.button.button--small.candidate-pod__details__reject-btn
         {:disabled (= (:status approval) "rejected")
          :title (approval->str approval)
          :class (when (= "rejected" updating) "button--light button--loading")
          :on-click #(do (.preventDefault %)
                         (dispatch [:candidates/set-approval-status object-id email "rejected"]))} "Reject"]]]
      [:div.column.candidate-pod__tag-section
       (if (= "company" type)
         [link "Company dashboard" :company-dashboard :id company-id :class "a--underlined"]
         (into [:ul.tags]
             (for [{name :name} skills]
               [:li name])))
       [:div.candidate-pod__details__updated [:span "Updated: "] updated]
       [:button.button.button--grey.candidate-pod__tag-section__delete-btn.button--small
        {:class (when (= "delete" updating) "button--loading")
         :on-click (fn [e]
                     (when (js/confirm "Are you sure you want to delete user?")
                       (dispatch [:candidates/delete-user object-id email])))} "Delete"]]]]))

;TODO maybe consider updating the other component or merge it somehow with the other one
(defn multiple-buttons
  "An input widget allowing to pick zero or more of the given options.
  value should be a set (a sub-set of :options)."
  [value {:keys [options on-change]}]
  (into [:div.multiple-buttons]
        (for [{:keys [option label]} options]
          [:button.button
           (merge
             (when-not (contains? value option)
               {:class "button--light"})
             (when on-change
               {:on-click #(do (.preventDefault %)
                               (dispatch (conj on-change option)))}))
           label])))





(defn main []
  [:div.main
   [:h1 "Candidates"]
   [:section.candidates-header
    [:form.wh-form
     [:fieldset
      [:div.text-field-control
       [:input.input
        {:name         "search"
         :type         "text"
         :autoComplete "off"
         :placeholder  "Search candidates..."
         :value        (<sub [::subs/search-term])
         :on-change    #(dispatch-sync [::events/set-search-term (-> % .-target .-value)])}]]
      [:div.facet-selectors
       [multiple-buttons (<sub [::subs/verticals])
        {:options (<sub [::subs/verticals-options])
         :on-change [::events/toggle-vertical]}]
       [multiple-buttons (<sub [::subs/approval-statuses])
        {:options (<sub [::subs/approvals-options])
         :on-change [::events/toggle-approval-statuses]}]]]]]
   [:section
    (case (<sub [::subs/loading-state])
      :error [:p "Please reload the page to see candidates. If you see this error after reload, try to login again."]
      :loading [:div.candidate-pod__loader [loader]]
      :no-results [:p "No candidates found."]
      [:div
       (into [:div]
            (for [candidate (<sub [::subs/search-results])]
              [candidate-pod candidate]))
       [pagination/pagination (<sub [::subs/current-page]) (<sub [::subs/pagination]) :candidates (<sub [::subs/query-params])]])]])

(defn page []
  (if (<sub [:user/admin?])
    [main]
    [:div.dashboard
     [not-found/not-found-page]]))
