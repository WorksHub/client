(ns wh.jobs.jobsboard.views
  (:require
    [rcslider :as slider]
    [re-frame.core :refer [dispatch]]
    [reagent.core :as r]
    [wh.common.emoji :as emoji]
    [wh.components.cards.views :refer [job-card]]
    [wh.components.forms.views :refer [labelled-checkbox multiple-checkboxes select-field text-input text-field radio-buttons]]
    [wh.components.icons :refer [icon]]
    [wh.components.loader :refer [loader]]
    [wh.components.pagination :as pagination]
    [wh.jobs.jobsboard.events :as events]
    [wh.jobs.jobsboard.subs :as subs]
    [wh.subs :refer [<sub]]
    [wh.util :as util])
  (:require-macros [clojure.core.strint :refer [<<]]))

(def slider-obj (aget js/window "rc-slider"))
(def slider (r/adapt-react-class slider-obj))
(def create-slider-with-tooltip (aget slider-obj "createSliderWithTooltip"))
(def slider-range (r/adapt-react-class (create-slider-with-tooltip (aget slider-obj "Range"))))

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
       " (clear location filters)"]
      )]
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
                    :day 50
                    :year 1000
                    1)
       :on-change #(dispatch [:wh.search/set-salary-range (js->clj %)])}])
   (<sub [:wh.search/salary-range-desc])
   [:span.wh-form
    [select-field (<sub [:wh.search/currency])
     {:options   (<sub [:wh.search/currencies])
      :disabled  (not (<sub [:wh.search/salary-type]))
      :on-change [:wh.search/set-currency]}]]
   [icon "close"
    :class "search__reset"
    :on-click #(dispatch [:wh.search/reset-salary])]])

(defn admin-filters-box []
  [:div.search__box.search__admin
   [multiple-checkboxes (<sub [:wh.search/published])
    {:options   (<sub [:wh.search/published-options])
     :on-change [:wh.search/toggle-published]}]
   [labelled-checkbox (<sub [:wh.search/only-mine])
    (assoc (<sub [:wh.search/only-mine-desc])
      :value (<sub [:wh.search/only-mine])
      :on-change [:wh.search/toggle-only-mine])]])

(defn search-box []
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
    (when (<sub [:user/admin?])
      [admin-filters-box])]])

(defn promoted-jobs []
  (into
    [:section.promoted-jobs
     [:h2 "Promoted Jobs"]]
    (let [parts (partition-all 3 (<sub [::subs/promoted-jobs]))]
      (for [part parts]
        (into [:div.columns]
              (for [job part]
                [:div.column.is-4
                 [job-card job :public (<sub [::subs/show-public-only?])]]))))))


(defn skeleton-jobs []
  [:section
   [:div.columns
    [:div.column.is-4 [job-card {:id (str "skeleton-job-" 1)}]]
    [:div.column.is-4 [job-card {:id (str "skeleton-job-" 2)}]]
    [:div.column.is-4 [job-card {:id (str "skeleton-job-" 3)}]]]])

(defn jobs-board []
  (let [jobs (<sub [::subs/jobs])]
    [:section
     (if (<sub [:wh.search/searching?])
       [skeleton-jobs]
       (when-let [parts (seq (partition-all 3 jobs))]
         [:div
          (doall
            (for [part parts]
              [:div.columns {:key (random-uuid)}
               (doall
                 (for [job part]
                   [:div.column.is-4 {:key (str "col-" (:id job))}
                    [job-card job :public (<sub [::subs/show-public-only?])]]))]))]))
     (when (and (not (<sub [:wh.search/searching?]))
                (seq jobs))
       [pagination/pagination
        (<sub [::subs/current-page])
        (<sub [::subs/pagination])
        :jobsboard
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
  (let [logged-in? (<sub [:user/logged-in?])
        searching? (<sub [:wh.search/searching?])]
    [:div.main.jobs-board
     (if logged-in?
       [search-box]
       [header false])
     [:div.search-results
      (when (and (seq (<sub [::subs/promoted-jobs]))
                 (not searching?)
                 logged-in?)
        [promoted-jobs])
      (if-let [job-list-header (<sub [::subs/job-list-header])]
        [:h2 {:class (util/merge-classes "job-list-header"
                                         (when searching? "skeleton"))}
         job-list-header]
        [:h3 {:class (util/merge-classes "search-result-count"
                                         (when searching?
                                           "skeleton"))}
         (<sub [::subs/result-count-str])])
      [jobs-board]]]))


(defn pre-set-search-page []
  [:div.main.jobs-board__pre-set-search
   [header true]
   [:div.search-results
    [:h3 {:class (util/merge-classes "search-result-count"
                                     (when (<sub [:wh.search/searching?])
                                       "skeleton"))}
     (<sub [::subs/pre-set-search-result-count-str])]
    [jobs-board]]])
