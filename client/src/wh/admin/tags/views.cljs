(ns wh.admin.tags.views
  (:require
    [reagent.core :as r]
    [wh.admin.tags.events :as events]
    [wh.admin.tags.subs :as subs]
    [wh.common.text :as text]
    [wh.components.forms.views :refer [text-field select-field]]
    [wh.components.icons :refer [icon]]
    [wh.components.not-found :as not-found]
    [wh.components.pagination :as pagination]
    [wh.components.tag :as tag]
    [wh.re-frame.events :refer [dispatch dispatch-sync]]
    [wh.re-frame.subs :refer [<sub]]
    [wh.util :as util]))

(defn tag-row
  [_tag]
  (let [temp-label  (r/atom nil)
        temp-weight (r/atom nil)
        editing?    (r/atom false)]
    (fn [{:keys [id label slug type subtype weight] :as tag}]
      [:div.edit-tags__tag-row
       {:class (when @editing? "edit-tags__tag-row--editing")}
       [:div.edit-tags__tag-row__primary
        [:div.is-flex.is-full-width
         [:ul.tags [tag/tag :li tag]]
         [:span (pr-str (dissoc tag :id))]]
        [:a.a--underlined
         {:on-click #(swap! editing? not)}
         (if @editing? "Hide" "Edit")]]
       (when @editing?

         [:div.edit-tags__tag-row__editable
          [:div.is-flex.tag-label [:strong "Label"] [text-field (or @temp-label label)
                                                     {:on-change #(reset! temp-label %)}]
           (when (and @temp-label (not= @temp-label label))
             [:div.edit-tags__tag-row__info__icons
              [icon "tick"
               :on-click #(dispatch [::events/set-tag-label tag @temp-label])]
              [icon "close"
               :on-click #(reset! temp-label nil)]])]
          [:div.is-flex.tag-type [:strong "Type"] [select-field type
                                                   {:options   (<sub [::subs/tag-types])
                                                    :on-change [::events/set-tag-type tag]}]]
          (when-let [subtypes (<sub [::subs/tag-subtypes type])]
            [:div.is-flex.tag-subtype [:strong "Subtype"] [select-field subtype
                                                           {:options   subtypes
                                                            :on-change [::events/set-tag-subtype tag]}]])
          [:div.is-flex.tag-slug [:strong "Slug"] [text-field slug
                                                   {:read-only true}]]
          [:div.is-flex.tag-weight [:strong "Weight"] [text-field (or @temp-weight weight)
                                                       {:type      :number
                                                        :maxv      1.0
                                                        :minv      0.0
                                                        :step      0.001
                                                        :on-change #(reset! temp-weight (or % ""))}]
           (when (and @temp-weight
                      (text/not-blank @temp-weight)
                      (not= @temp-weight weight))
             [:div.edit-tags__tag-row__info__icons
              [icon "tick"
               :on-click #(dispatch [::events/set-tag-weight tag @temp-weight])]
              [icon "close"
               :on-click #(reset! temp-weight nil)]])]
          [:div.is-flex.tag-weight-slider [:strong ""]
           [:input {:type :range
                    :min 0.0
                    :max 1.0
                    :step 0.001
                    :value (or @temp-weight weight)
                    :on-change #(reset! temp-weight (js/parseFloat (.. % -target -value)))}]]])

       #_[:div.edit-tags__tag-row__actions ;; TODO add ability to delete tags?
          [icon "delete"
           :on-click #(dispatch [::events/delete-tag id])]]])))

(def page-limit 30)

(defn main []
  (fn []
    (let [all-tags (<sub [::subs/all-tags])
          query-params (<sub [:wh.subs/query-params])
          current-page (js/parseInt (get query-params "page" "1"))
          {:keys [tags total]} (<sub [::subs/all-tags--filtered current-page page-limit])]
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
       [:span "Showing " (inc (* (dec current-page) page-limit)) "-" (min (* current-page page-limit)
                                                                          (count all-tags)) " of " (count all-tags)]
       [:section.edit-tags__list
        (if all-tags
          (if (not-empty tags)
            [:ul
             (doall
               (for [tag tags]
                 ^{:key (:id tag)}
                 [:li [tag-row tag]]))]
            [:h2 "No matching tags"])
          [:h2 "Loading..."])]
       (when (and (not-empty tags) (> total page-limit))
         [pagination/pagination current-page (pagination/generate-pagination current-page (int (js/Math.ceil (/ total page-limit)))) :tags-edit query-params])])))

(defn page []
  (if (<sub [:user/admin?])
    [main]
    [:div.dashboard
     [not-found/not-found-page]]))
