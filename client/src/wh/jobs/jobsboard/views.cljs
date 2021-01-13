(ns wh.jobs.jobsboard.views
  (:require ["rc-slider" :as slider]
            [re-frame.core :refer [dispatch]]
            [reagent.core :as r]
            [wh.common.emoji :as emoji]
            [wh.components.forms.views :refer [labelled-checkbox multiple-checkboxes select-field text-input text-field radio-buttons]]
            [wh.components.icons :refer [icon]]
            [wh.components.job :refer [job-card]]
            [wh.components.pagination :as pagination]
            [wh.jobs.jobsboard.events :as events]
            [wh.jobs.jobsboard.subs :as subs]
            [wh.jobsboard-ssr.db :as jobsboard-ssr]
            [wh.subs :refer [<sub]]
            [wh.util :as util])
  (:require-macros [clojure.core.strint :refer [<<]]))

(def slider-range (r/adapt-react-class (slider/createSliderWithTooltip slider/Range)))

(defn city-list [cities]
  (doall
    (for [{:keys [value _count]} cities]
      ^{:key (str "city-" value)}
      [:div.search__city
       [labelled-checkbox (<sub [:wh.search/city value])
        {:value     value,
         :label     value,
         :on-change [:wh.search/toggle-city value]}]])))

(defn flag [code]
  (when code
    [:span.search__flag (emoji/country-code->emoji code)]))

(defn region-box [wh-region subdivide?]
  (let [countries-and-cities (<sub [:wh.search/country-list wh-region])
        region-name (subs/region-names wh-region)
        region-flag (subs/region-flags wh-region)
        country-names (<sub [:wh.search/country-names])]
    [:div.search__box.search__region
     [labelled-checkbox (<sub [:wh.search/wh-region wh-region])
      (let [count (<sub [:wh.search/wh-region-count (name wh-region)])]
        {:value     region-name,
         :disabled  (not count),
         :label     [:span region-name " " [flag region-flag]],
         :on-change [:wh.search/toggle-wh-region wh-region]})]
     (if subdivide?
       (doall
         (for [{:keys [value count cities]} countries-and-cities]
           (if (and (not (next cities))
                    (= count (:count (first cities))))
             (first (city-list cities))
             ^{:key (str "country-" value)}
             [:div.search__country
              [labelled-checkbox
               (<sub [:wh.search/country value])
               {:value     value
                :label     [:span (country-names value) " " [flag value]]
                :on-change [:wh.search/toggle-country value]}]
              (city-list cities)])))
       (city-list (mapcat :cities countries-and-cities)))]))

(defn expanded-location-box [collapsed?]
  [:div.search__location-wrapper
   [:div.search__location
    [region-box :europe true]
    [region-box :us false]
    [region-box :rest-of-world true]]
   [icon "roll-down"
    :class "search__roll search__location-roll--expanded"
    :on-click #(swap! collapsed? not)]])

(defn collapsed-location-box [collapsed?]
  [:div.search__location--collapsed.search__box
   {:on-click #(swap! collapsed? not)}
   [:span.label "Location: "]
   [:span.search__location-description
    (<sub [:wh.search/collapsed-location-description])]
   [:span.search__location-hint
    (if (<sub [:wh.search/show-search-everywhere?])
      " (click to expand and filter)"
      [:a.search__location-hint
       {:on-click #(do (.stopPropagation %)
                       (dispatch [:wh.search/clear-locations]))}
       " (clear location filters)"])]

   [icon "roll-down"
    :class "search__roll"]
   (when-not (<sub [:wh.search/show-search-everywhere?])
     [icon "close"
      :class "search__reset"
      :on-click #(do
                   (.stopPropagation %)
                   (dispatch [:wh.search/reset-locations]))])])

(defn location-box []
  (let [collapsed? (r/atom true)]
    (fn []
      (if @collapsed?
        [collapsed-location-box collapsed?]
        [expanded-location-box collapsed?]))))

(defn salary-box []
  [:<>
   [:div.search__box.search__salary
    [:span.label "Salary:"]
    [radio-buttons (<sub [:wh.search/salary-type])
     {:options   [{:id :day, :label "Daily"}
                  {:id :year, :label "Yearly"}]
      :on-change [:wh.search/set-salary-type]}]
    (when (<sub [:wh.search/grouped-salary-ranges])
      [slider-range
       {:value     (<sub [:wh.search/salary-range-js])
        :min       (<sub [:wh.search/salary-min])
        :max       (<sub [:wh.search/salary-max])
        :step      (case (<sub [:wh.search/salary-type])
                     :day  50
                     :year 1000
                     1)
        :on-change #(dispatch [:wh.search/set-salary-range (js->clj %)])}])
    [:span.search__salary__range
     (<sub [:wh.search/salary-range-desc])]
    [:span.wh-form
     [select-field (<sub [:wh.search/currency])
      {:options   (<sub [:wh.search/currencies])
       :disabled  (not (<sub [:wh.search/salary-type]))
       :on-change [:wh.search/set-currency]}]]
    [icon "close"
     :class "search__reset"
     :on-click #(dispatch [:wh.search/reset-salary])]]
   [:div.search__box.search__salary-extra
    [labelled-checkbox (<sub [:wh.search/show-competitive?])
     {:label     "Show jobs with 'Competitive' salary"
      :value     (<sub [:wh.search/show-competitive?])
      :on-change [:wh.search/toggle-show-competitive]}]]])

(defn admin-filters-box []
  [:div.search__box.search__admin
   [multiple-checkboxes (<sub [:wh.search/published])
    {:options   (<sub [:wh.search/published-options])
     :on-change [:wh.search/toggle-published]}]
   [labelled-checkbox (<sub [:wh.search/only-mine])
    (assoc (<sub [:wh.search/only-mine-desc])
           :value (<sub [:wh.search/only-mine])
           :on-change [:wh.search/toggle-only-mine])]])

(def view-types
  {:view/list  {:icon-name "applications" :label "List"}
   :view/cards {:icon-name "jobs-board" :label "Cards"}})

(defn list-view-type [view-type]
  [:div.search__box.search__view-type
   [:span.label "View: "]
   (for [[type {:keys [label icon-name] :as value}] view-types]
     (let [selected (= (name type) (name view-type))]
       ^{:key type}
       [:p.search__view-type__button
        {:on-click #(dispatch [:wh.events/nav--set-query-param
                               jobsboard-ssr/view-type-param (name type)])
         :class (when selected :search__view-type__button--selected)}
        [icon icon-name]
        [:span label]]))])

(defn search-box []
  (let [view-type (<sub [::subs/view-type])]
    [:section.search
     [:form.search__form
      {:on-submit #(do (.preventDefault %)
                       (dispatch [:wh.search/search]))}
      [:div.search__box.search__tags
       {:class (when (<sub [:wh.search/tags-collapsed?])
                 "search__tags--collapsed")}
       [icon "roll-down"
        :class "search__roll search__tags-roll"
        :on-click #(dispatch [:wh.search/toggle-tags-collapsed])]
       [text-field (<sub [:wh.search/tag-part])
        {:label       "Tags:"
         :class       "search__tags-text"
         :placeholder "Type to search tags"
         :on-change   [:wh.search/set-tag-part]}]
       (into [:ul.tags]
             (map (fn [{:keys [tag count selected]} tag]
                    [:li {:class    (when selected "tag--selected")
                          :on-click #(dispatch [:wh.search/search-by-tag tag true])}
                     tag]))
             (<sub [:wh.search/matching-tags]))]
      [:div.search__box.search__perks
       [labelled-checkbox (<sub [:wh.search/sponsorship])
        (assoc (<sub [:wh.search/sponsorship-desc])
               :value (<sub [:wh.search/sponsorship])
               :on-change [:wh.search/toggle-sponsorship])]
       [labelled-checkbox (<sub [:wh.search/remote])
        (assoc (<sub [:wh.search/remote-desc])
               :value (<sub [:wh.search/remote])
               :on-change [:wh.search/toggle-remote])]]
      [:div.search__box.search__role-type
       [multiple-checkboxes (<sub [:wh.search/role-types])
        {:options   (<sub [:wh.search/available-role-types])
         :on-change [:wh.search/toggle-role-type]}]]
      [location-box]
      [salary-box]
      [list-view-type view-type]
      (when (<sub [:user/admin?])
        [admin-filters-box])]]))

(def jobs-container-class
  {:cards "jobs-board__jobs-list__content"
   :list  ""})

(defn promoted-jobs [view-type]
  (let [jobs         (<sub [::subs/promoted-jobs])
        logged-in?   (<sub [:user/logged-in?])
        has-applied? (some? (<sub [:user/applied-jobs]))
        company-id   (<sub [:user/company-id])
        admin?       (<sub [:user/admin?])]
    [:section.promoted-jobs.jobs-board__jobs-list
     [:h2 "Promoted Jobs"]

     [:div {:class (jobs-container-class view-type)}
      (for [job jobs]
        ^{:key (:id job)}
        [job-card job {:logged-in?        logged-in?
                       :view-type         view-type
                       :user-has-applied? has-applied?
                       :user-is-company?  (not (nil? company-id))
                       :user-is-owner?    (or admin? (= company-id (:company-id job)))
                       :apply-source      "jobsboard-promoted-job"}])]]))

(defn- skeleton-jobs [view-type]
  [:section.jobs-board__jobs-list
   [:div
    {:class (jobs-container-class view-type)}
    (for [i [1 2 3]]
      ^{:key i}
      [job-card {:id (str "skeleton-job-" i)}
       {:view-type view-type}])]])

(defn jobs-board [view-type preset-search?]
  (let [jobs         (<sub [::subs/jobs])
        logged-in?   (<sub [:user/logged-in?])
        has-applied? (some? (<sub [:user/applied-jobs]))
        company-id   (<sub [:user/company-id])
        admin?       (<sub [:user/admin?])
        searching?   (<sub [:wh.search/searching?])]
    [:section.jobs-board__jobs-list
     (if searching?
       [skeleton-jobs view-type]
       (when jobs
         [:div
          {:class (jobs-container-class view-type)}
          (for [job jobs]
            ^{:key (:id job)}
            [job-card job {:logged-in?        logged-in?
                           :view-type         view-type
                           :user-has-applied? has-applied?
                           :user-is-company?  (not (nil? company-id))
                           :user-is-owner?    (or admin? (= company-id (:company-id job)))
                           :apply-source      "jobsboard-job"}])]))
     (when (and (not (<sub [:wh.search/searching?]))
                (seq jobs))
       [pagination/pagination
        (<sub [::subs/current-page])
        (<sub [::subs/pagination])
        (when-not preset-search? :jobsboard)
        (<sub [::subs/pagination-query-params])])]))

(defn header [preset-search?] ;; TODO once SSR is finished, remove this component and only use CLCJ one
  (let [filter-shown? (<sub [::subs/filter-shown?])]
    [:div.jobs-board__header
     [:div {:class (when (and preset-search? (<sub [:wh.search/searching?])) "skeleton")}
      [:h1 (<sub [::subs/header-title])]
      [:h2 (<sub [::subs/header-subtitle])]
      [:h3 (<sub [::subs/header-description])]
      [:a {:class    "jobs-board__header__filter-toggle a--capitalized-red a--hover-red"
           :on-click #(dispatch [::events/toggle-filter])}
       (if filter-shown?
         [:div
          [icon "minus" :class "search__toggle"]
          [:span "Hide filters"]]
         [:div
          [icon "plus" :class "search__toggle"]
          [:span "Show filters"]])]]
     (when filter-shown?
       [search-box])]))

(defn page []
  (let [logged-in?     (<sub [:user/logged-in?])
        searching?     (<sub [:wh.search/searching?])
        view-type      (<sub [::subs/view-type])
        preset-search? false]
    [:div.main.jobs-board
     (if logged-in?
       [search-box]
       [header preset-search?])
     [:div.search-results
      (when (and (seq (<sub [::subs/promoted-jobs]))
                 (not searching?)
                 logged-in?)
        [promoted-jobs view-type])
      (if-let [job-list-header (<sub [::subs/job-list-header])]
        [:h2 {:class (util/merge-classes "job-list-header"
                                         (when searching? "skeleton"))}
         job-list-header]
        [:h3 {:class (util/merge-classes "search-result-count"
                                         (when searching?
                                           "skeleton"))}
         (<sub [::subs/result-count-str])])
      [jobs-board view-type preset-search?]]]))


(defn pre-set-search-page []
  (let [view-type      (<sub [::subs/view-type])
        preset-search? true]
    [:div.main.jobs-board__pre-set-search
     [header preset-search?]
     [:div.search-results
      [:h3 {:class (util/merge-classes "search-result-count"
                                       (when (<sub [:wh.search/searching?])
                                         "skeleton"))}
       (<sub [::subs/pre-set-search-result-count-str])]
      [jobs-board view-type preset-search?]]]))
