(ns wh.admin.tags.views
  (:require
    [reagent.core :as r]
    [wh.admin.tags.events :as events]
    [wh.admin.tags.subs :as subs]
    [wh.components.forms.views :refer [text-field select-field]]
    [wh.components.icons :refer [icon]]
    [wh.components.not-found :as not-found]
    [wh.re-frame.events :refer [dispatch dispatch-sync]]
    [wh.re-frame.subs :refer [<sub]]
    [wh.util :as util]))

(defn tag-row
  [_tag]
  (let [temp-label (r/atom nil)]
    (fn [{:keys [id label slug type subtype] :as tag}]
      [:div.edit-tags__tag-row
       [:ul.tags [:li.tag label]]
       [:div.edit-tags__tag-row__info
        [:div.is-flex [:strong "Label"] [text-field (or @temp-label label)
                                         {:on-change #(reset! temp-label %)}]
         (when (and @temp-label (not= @temp-label label))
           [:div.edit-tags__tag-row__info__icons
            [icon "tick"
             :on-click #(dispatch [::events/set-tag-label tag @temp-label])]
            [icon "close"
             :on-click #(reset! temp-label nil)]])]
        [:div.is-flex [:strong "Type"] [select-field type
                                        {:options (<sub [::subs/tag-types])
                                         :on-change [::events/set-tag-type tag]}]]
        (when-let [subtypes (<sub [::subs/tag-subtypes type])]
          [:div.is-flex [:strong "Subtype"] [select-field subtype
                                             {:options subtypes
                                              :on-change [::events/set-tag-subtype tag]}]])
        [:div.is-flex [:strong "Slug"] [:input {:type :text
                                                :read-only true
                                                :value slug}]]]
       #_[:div.edit-tags__tag-row__actions ;; TODO add ability to delete tags?
          [icon "delete"
           :on-click #(dispatch [::events/delete-tag id])]]])))

(defn main []
  (let [all-tags (<sub [::subs/all-tags])
        tags (not-empty (<sub [::subs/all-tags--filtered 30]))]
    [:div.main.edit-tags
     [:h1 "Edit Tags"]
     [:section
      [:form.wh-formx.is-flex
       [:div.text-field-control
        [:input.input
         {:name         "search"
          :type         "text"
          :autoComplete "off"
          :placeholder  "Search tags..."
          :value        (<sub [::subs/search-term])
          :on-change    #(dispatch-sync [::events/set-search-term (-> % .-target .-value)])}]]
       [select-field (<sub [::subs/type-filter])
        {:options (into [{:id nil :label "All tags"}] (<sub [::subs/tag-types]))
         :on-change [::events/set-type-filter]}]]]
     [:span "Showing " (count tags) " of " (count all-tags)]
     [:section.edit-tags__list
      (if all-tags
        (if tags
          [:ul
           (doall
             (for [tag tags]
               ^{:key (:id tag)}
               [:li [tag-row tag]]))]
          [:h2 "No matching tags"])
        [:h2 "Loading..."])]]))

(defn page []
  (if (<sub [:user/admin?])
    [main]
    [:div.dashboard
     [not-found/not-found-page]]))
