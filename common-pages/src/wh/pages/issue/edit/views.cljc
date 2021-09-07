(ns wh.pages.issue.edit.views
  (:require #?(:cljs [wh.components.forms.views :refer [text-input]])
            #?(:cljs [wh.components.overlay.views :refer [popup-wrapper]])
            [wh.components.common :refer [link img wrap-img]]
            [wh.components.forms :as forms]
            [wh.components.icons :refer [icon]]
            [wh.components.issue :as issue]
            [wh.components.selector :refer [selector]]
            [wh.pages.issue.edit.events :as events]
            [wh.pages.issue.edit.subs :as subs]
            [wh.re-frame.events :refer [dispatch]]
            [wh.re-frame.subs :refer [<sub]]))

(defn edit-dialog []
  #?(:cljs
     (let [{:keys [title company repo status]} (<sub [::subs/current-issue])
           updating? (<sub [::subs/updating?])
           logo (:logo company)]
       [popup-wrapper
        {:id :edit-issue
         :on-close #(do
                      (dispatch [::events/set-pending-level nil])
                      (dispatch [::events/close-issue-edit-popup]))
         :codi? false}
        [:div.issue__edit
         [:div.issue__edit__header
          [:div.issue__edit__top-info
           [:a (str (:owner repo) " / " (:name repo))]]
          [:div.is-flex.issue__edit__title-container
           (when logo
             (wrap-img img logo
                       {:alt (str (:name company) " logo")
                        :w 64 :h 64 :fit "clip" :class "issue__logo"}))
           [:span.issue__edit__title title]]]
         [:div.issue__edit__status
          [selector (<sub [::subs/pending-status])
           [[:open "Open"] [:closed "Closed"]]
           #(dispatch [::events/set-pending-status %])]]
         [:div.issue__edit__level
          [:div.issue__edit__label "Level:"]
          [:div.wh-formx
           [forms/radio-buttons (<sub [::subs/pending-level])
            {:options   (map #(hash-map :id % :label (issue/level->str %))
                             [:beginner :intermediate :advanced])
             :on-change [::events/set-pending-level]}]]]
         [:div.issue__edit__compensation
          [:div.issue__edit__label "Value in US$:"]
          [:div.wh-formx
           #?(:cljs
              [:div.text-field-control
               [text-input (<sub [::subs/pending-compensation])
                {:on-change [::events/set-pending-compensation]
                 :type :number}]])]]
         [:div.issue__edit__buttons.columns
          {:on-click (when-not updating? #(dispatch [::events/save-issue]))}
          [:button.button.column "Update issue"]]
         (when updating?
           [:div.issue__edit__updating
            {}
            [:div.issue__edit__updating__spinner]])]])))

(defn confirm-dialog []
  #?(:cljs
     (let [updating? (<sub [::subs/updating?])]
       [popup-wrapper
        {:id :edit-issue-confirm
         :on-close #(dispatch [::events/close-issue-edit-popup])
         :codi? true}
        [:div.issue__confirm-save
         [:p "This will also update issue status in GitHub. Please confirm you want to do that."]
         [:div.issue__confirm-save__buttons.columns
          [:button.button.button--light.column
           {:on-click #(dispatch [::events/close-issue-edit-popup])}
           "Cancel"]
          [:button.button.column
           {:on-click (when-not updating? #(dispatch [::events/save-issue true]))}
           "Change"]]
         (when updating?
           [:div.issue__edit__updating
            {}
            [:div.issue__edit__updating__spinner]])]])))

(defn edit-issue []
  #?(:cljs
     (case (<sub [:edit-issue/displayed-dialog])
       :edit [edit-dialog]
       :confirm [confirm-dialog]
       nil)))
