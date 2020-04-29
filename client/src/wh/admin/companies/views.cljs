(ns wh.admin.companies.views
  (:require
    [re-frame.core :refer [dispatch]]
    [wh.admin.companies.events :as events]
    [wh.admin.companies.subs :as subs]
    [wh.common.data :refer [get-manager-name package-data]]
    [wh.components.common :refer [link img wrap-img]]
    [wh.components.forms.views :as forms]
    [wh.components.icons :refer [icon]]
    [wh.subs :refer [<sub]]
    [wh.verticals :as verticals]))

(defn company-card
  [{:keys [name id logo manager vertical job-stats package application-state-frequencies] :as company}]
  (let [skeleton? (nil? company)
        pending (:pending application-state-frequencies)]
    [:div.card.companies__company-card.columns
     {:class (when skeleton? "skeleton")}
     [:div.column.is-flex
      [:div.logo
       (if logo
         (wrap-img img logo {})
         [:div.empty-logo])]
      [:div.info
       [:div.title [link name :company-dashboard :id id :class "a--hover-red"]]
       [:div.company-details
        [:span "Manager:"]  [:span (or (get-manager-name manager) [:i "Unassigned"])]
        [:span "Vertical:"] [:span (if vertical (verticals/config vertical :platform-name) "None")]]]]
     [:div.column.is-narrow.stats
      [:div.counts
       [:div.count [icon (if skeleton? "circle" "applications")] (:applications job-stats)]
       [:div.count [icon (if skeleton? "circle" "like")]         (:likes job-stats)]
       [:div.count [icon (if skeleton? "circle" "views")]        (:views job-stats)]]
      [:div.package
       {:style {:background-image (str "url(" (get-in package-data [package :img :src]) ")")}}]
      (when (and pending (pos? pending))
        [:div.pending
         [link (str "View " pending " Pending") :admin-company-applications :id id :class "a--underlined"]])]]))

(defn page
  []
  (let [results       (<sub [::subs/results])
        loading-more? (<sub [::subs/loading-more?])
        collapsed?    (<sub [::subs/tag-search-collapsed?])
        companies'    (or results [])
        companies     (if (or (not results) loading-more?)
                        (concat (or results []) (map #(hash-map :id % :skeleton? true) (range 10)))
                        results)
        tags (<sub [::subs/matching-tags])]
    [:div.main.companies
     [:h1 "Companies"]
     [:form.wh-formx
      {:on-submit (fn [e]
                    (when-let [search (some #(when (and (= :search (:type %))
                                                        (not (:selected %))) (:tag %)) tags)]
                      (dispatch [::events/toggle-tag search]))
                    (.preventDefault e))}
      [forms/tags-field
       (<sub [::subs/tag-search])
       {:id                 "companies-tags"
        :collapsed?         (if (nil? collapsed?) true collapsed?)
        :placeholder        "Type to add filters for vertical, manager or package"
        :tags               tags
        :on-change          [::events/set-tag-search]
        :on-toggle-collapse #(dispatch [::events/toggle-tag-search-collapsed])
        :on-tag-click       #(dispatch [::events/toggle-tag (:tag %)])}]
      [:div.companies__beneath-tags.is-flex
       (let [[current-count total-count] (<sub [::subs/company-counts])]
         [:h3 (if (and current-count total-count)
                [:i "Showing " current-count " of " total-count " matching companies"])])
       [:div.companies__sort-dropdown.is-flex
        [:span "Sort by"]
        [forms/select-field (<sub [::subs/sort])
         {:read-only (nil? results)
          :on-change [::events/select-sort]
          :options (<sub [::subs/sort-options])}]]]]
     (if (empty? companies)
       [:div.companies__empty
        [:h3 "No companies match your search terms."]]
       [:div.companies__results
        (for [company companies]
          ^{:key (:id company)}
          [:div.companies__result
           [company-card (if (:skeleton? company) nil company)]])])
     (when (<sub [::subs/show-load-more?])
       [:div.companies__load-more
        [:button.button.button--inverted
         {:on-click #(dispatch [::events/load-more-companies])}
         "Load More"]])]))
