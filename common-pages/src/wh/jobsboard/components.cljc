(ns wh.jobsboard.components
  (:require #?(:cljs ["rc-slider" :as slider])
            #?(:cljs [reagent.core :as r])
            #?(:cljs [wh.jobs.jobsboard.subs :as subs]
               :clj [wh.jobsboard.subs :as subs])
            [wh.components.buttons-page-navigation :as buttons-page-navigation]
            [wh.components.forms :as forms]
            [wh.components.icons :as icons :refer [icon]]
            [wh.components.job-new :as job]
            [wh.components.side-card.side-card :as side-cards]
            [wh.interop :as interop]
            [wh.interop.forms :as interop-forms]
            [wh.jobsboard.events :as events]
            [wh.re-frame.events :refer [dispatch dispatch-sync]]
            [wh.re-frame.subs :refer [<sub]]
            [wh.routes :as routes]
            [wh.jobsboard.styles :as styles]
            [wh.util :as util]))

(defn section-title [children]
  [:h1 (util/smc styles/search-box__title) children])

(defn clear-section [on-clear]
  [:a (merge (util/smc styles/clear-section)
             (interop/on-click-fn on-clear))
   [:span "clear"]])

(defn section-header [{:keys [title pristine? on-clear] :as _props}]
  [:div (util/smc styles/section-header)
   [section-title title]

   (when-not pristine?
     [clear-section on-clear])])

(defn query-box* [{:keys [on-clear pristine? value on-change on-enter]}]
  [:div (util/smc styles/query)
   [section-header {:title     "Search within jobs"
                    :on-clear  on-clear
                    :pristine? pristine?}]

   [forms/text-input
    value
    {:class-input styles/input
     :placeholder "Type to search"
     :on-change   on-change
     :on-enter    on-enter}]])

(defn query-box []
  [query-box*
   {:on-clear  #?(:cljs #(do
                           (dispatch [:wh.search/set-query nil])
                           (dispatch [:wh.search/search]))
                  :clj nil)
    :pristine? #?(:cljs (<sub [::subs/query-pristine?])
                  :clj true)
    :value     #?(:cljs (<sub [::subs/current-query])
                  :clj "")
    :on-change #?(:cljs #(dispatch [:wh.search/set-query (aget % "target" "value")])
                  :clj nil)
    :on-enter  (interop-forms/add-input-value-to-url :search :wh.search/search)}])

(defn location-box* [{:keys [on-clear pristine? locations selected
                             tags-url init-from-js-tags? on-tag-select]}]
  [:div (util/smc styles/location)
   [section-header {:title     "Location"
                    :on-clear  on-clear
                    :pristine? pristine?}]

   [forms/tags-filter-field
    (merge {:id                 "location-box"
            :solo?              true
            :show-icons         false
            :placeholder        "Type to search locations"
            :selected           selected
            :new-design?        true
            :init-from-js-tags? init-from-js-tags?}
           (when locations {:tags locations})
           (when on-tag-select {:on-tag-select on-tag-select})
           (when tags-url {:tags-url tags-url}))]])

(defn location-box []
  [location-box*
   (merge {:on-tag-select (interop-forms/add-tag-value-to-url ::events/on-location-select)}
          #?(:clj {:pristine?          true
                   :init-from-js-tags? true
                   :tags-url           (routes/path :tags-collection :params {:id :locations})}
             :cljs {:on-clear  #(dispatch [:wh.search/clear-locations-and-perks])
                    :pristine? (<sub [::subs/locations-pristine?])
                    :locations (<sub [:wh.search/available-locations])
                    :selected  (<sub [::subs/selected-locations])}))])

(defn tags-box* [{:keys [on-clear pristine? tags selected
                         tags-url init-from-js-tags? on-tag-select]}]
  [:div (util/smc styles/tags)
   [section-header {:title     "Tags"
                    :on-clear  on-clear
                    :pristine? pristine?}]

   [forms/tags-filter-field
    (merge {:id                 "tags-box"
            :solo?              true
            :show-icons         false
            :placeholder        "Type to search tags"
            :selected           selected
            :new-design?        true
            :init-from-js-tags? init-from-js-tags?}
           (when tags {:tags tags})
           (when on-tag-select {:on-tag-select on-tag-select})
           (when tags-url {:tags-url tags-url}))]])

(defn tags-box []
  [tags-box*
   (merge {:on-tag-select (interop-forms/add-tag-value-to-url ::events/on-tag-select)}
          #?(:clj {:pristine?          true
                   :init-from-js-tags? true
                   :tags-url           (routes/path :tags-collection :params {:id :jobs})}
             :cljs {:on-clear  #(dispatch [:wh.search/clear-tags])
                    :pristine? (<sub [::subs/tags-pristine?])
                    :tags      (<sub [:wh.search/visible-tags])
                    :selected  (<sub [:wh.search/selected-tags])}))])

(defn role-type-box* [{:keys [on-clear pristine? values on-change options]}]
  [:div (util/smc styles/role-types)
   [section-header {:title     "Role type"
                    :on-clear  on-clear
                    :pristine? pristine?}]

   [forms/multiple-checkboxes values
    {:options        options
     :on-change      on-change
     :class-wrapper  styles/checkboxes
     :label-class    styles/checkboxes__item__label
     :selected-class styles/checkboxes__item--selected
     :item-class     styles/checkboxes__item}]])

(defn role-type-box []
  [role-type-box*
   #?(:clj {:pristine? true
            :values    #{}
            :on-change #(interop-forms/add-check-value-to-url :role-type %)
            :options   (<sub [::subs/role-types])}
      :cljs {:on-clear  #(dispatch [:wh.search/clear-role-types])
             :pristine? (<sub [::subs/role-types-pristine?])
             :values    (<sub [:wh.search/role-types])
             :on-change [:wh.search/toggle-role-type]
             :options   (<sub [:wh.search/available-role-types])})])

(defn perks-box* [perks]
  [:div (util/smc styles/perks)
   (map-indexed
     (fn [idx perk-checkbox]
       ^{:key idx}
       [forms/labelled-checkbox perk-checkbox]) perks)])

(defn perks-box []
  (let [remote?      #?(:cljs (<sub [:wh.search/remote]) :clj false)
        sponsorship? #?(:cljs (<sub [:wh.search/sponsorship]) :clj false)]
    [perks-box*
     [{:value       remote?
       :label       [:<> [:span "Remote"]
                     [icon "globe" :class styles/checkboxes__icon]]
       :on-change   #?(:cljs [:wh.search/toggle-remote]
                       :clj (interop-forms/add-check-value-to-url :remote true))
       :class       (util/mc styles/checkboxes__item
                             [remote? styles/checkboxes__item--selected])
       :label-class styles/checkboxes__item__label}

      {:value       sponsorship?
       :label       [:<> [:span "Sponsorship offered"]
                     [icon "award" :class styles/checkboxes__icon]]
       :on-change   #?(:cljs [:wh.search/toggle-sponsorship]
                       :clj (interop-forms/add-check-value-to-url :sponsorship-offered true))
       :class       (util/mc styles/checkboxes__item
                             [sponsorship? styles/checkboxes__item--selected])
       :label-class styles/checkboxes__item__label}]]))

#?(:cljs
   (def slider-range
     (r/adapt-react-class slider/default)))

(defn salary-box []
  [:div (util/smc styles/salary)
   [section-header
    {:title     "Compensation"
     :pristine? #?(:cljs (<sub [::subs/salary-pristine?]) :clj true)
     :on-clear  #(dispatch [:wh.search/reset-salary])}]
   [:div (util/smc styles/salary__wrapper)
    (let [options   [{:id :year, :label "Yearly"}
                     {:id :day, :label "Daily"}]
          id->label {"year" "Yearly"
                     "day"  "Daily"}]
      [forms/radio-buttons #?(:cljs (<sub [:wh.search/salary-type]) :clj nil)
       {:options   options
        :on-change #?(:cljs [:wh.search/set-salary-type]
                      :clj #(interop-forms/add-check-value-to-url
                              :remuneration.time-period (id->label (name %))))
        :class     styles/salary__type}])

    [:div (util/smc styles/salary__currency-selector-wrapper)
     (let [options #?(:cljs (<sub [:wh.search/currencies])
                      :clj (<sub [::subs/currencies]))]
       [forms/select-field
        {:options     options
         :value       #?(:cljs (<sub [:wh.search/currency]) :clj nil)
         :disabled    #?(:cljs (not (<sub [:wh.search/salary-type])) :clj true)
         :on-change   #?(:cljs #(let [value (nth options (js/parseInt (aget % "target" "value")))]
                                  (dispatch-sync [:wh.search/set-currency value]))
                         :clj (interop-forms/add-select-value-to-url
                                :remuneration.currency (map (fn [item] {:id item}) options)))
         :class       styles/salary__currency-selector
         :hide-error? true}])

     [:div (util/smc styles/salary__amount-wrapper)
      [:span (util/smc styles/salary__amount styles/salary__amount--left)
       #?(:cljs (<sub [:wh.search/salary-slider-min]) :clj "0")]
      [:span (util/smc styles/salary__hyphen) "—"]
      [:span (util/smc styles/salary__amount)
       #?(:cljs (<sub [:wh.search/salary-slider-max]) :clj "100k")]]]

    #?(:cljs (when (<sub [:wh.search/grouped-salary-ranges])
               [slider-range
                {:value     (<sub [:wh.search/salary-from])
                 :min       (<sub [:wh.search/salary-min])
                 :max       (<sub [:wh.search/salary-max])
                 :step      (case (<sub [:wh.search/salary-type])
                              :day  50
                              :year 1000
                              1)
                 :on-change #(dispatch [:wh.search/set-salary-from (js->clj %)])}])
       :clj [:span (util/smc styles/compensation-interval)
             "Choose compensation interval"])

    (let [competitive? #?(:cljs (<sub [:wh.search/show-competitive?]) :clj true)]
      [:div.search__box
       [forms/labelled-checkbox
        {:label       "Display 'Competitive' compensation"
         :value       competitive?
         :on-change   [:wh.search/toggle-show-competitive]
         :label-class styles/checkboxes__item__label
         :class       (util/mc styles/checkboxes__item
                               [competitive? styles/checkboxes__item--selected])}]])]])

(defn admin-filters-box []
  [:div
   ;; TODO CH5572. Implement pristine? and clearing admin section
   [section-header
    {:title     "Admin"
     :pristine? true}]

   [forms/multiple-checkboxes (<sub [:wh.search/published])
    {:options        (<sub [:wh.search/published-options])
     :on-change      [:wh.search/toggle-published]
     :class-wrapper  styles/checkboxes
     :label-class    styles/checkboxes__item__label
     :selected-class styles/checkboxes__item--selected
     :item-class     styles/checkboxes__item}]

   (let [mine? (<sub [:wh.search/only-mine])]
     [forms/labelled-checkbox
      (assoc (<sub [:wh.search/only-mine-desc])
             :value mine?
             :on-change [:wh.search/toggle-only-mine]
             :class (util/mc styles/checkboxes__item
                      [mine? styles/checkboxes__item--selected])
             :label-class styles/checkboxes__item__label)])])

(def toggle-filters (interop/on-click-fn
                      (interop/toggle-jobs-filters-display)))

(defn filters-header []
  [:div {:class styles/filter-header}
   [:div {:class styles/filter-header__title}
    "Filter"]
   [:button (merge
             {:class styles/filter-header__close
              :type  "button"}
             toggle-filters)
    [icons/icon "close" :class styles/filter-header__close-icon]]])


(defn search-box [{:keys [mobile?] :as args}]
  [:section#search-box (util/smc styles/search-box
                         [mobile? styles/search-box--overlay]
                         ;; search-box--hidden will be used by external js to toggle overlay
                         [mobile? "search-box--hidden"])
   [:form {:class     (util/mc styles/search-box__form
                        [mobile? styles/search-box__form--overlay])
           :id        "filter"
           :on-submit #?(:cljs #(do (.preventDefault %)
                                    (dispatch [:wh.search/search]))
                         :clj  "")}
    (when mobile? [filters-header])
    [query-box]
    [tags-box]
    [location-box]
    [perks-box]
    [salary-box]
    [role-type-box]
    (when (<sub [:user/admin?])
      [admin-filters-box])
    [:button
     (merge
      {:class (util/mc styles/bottom-button styles/bottom-button--apply)
       :type  "button"}
      toggle-filters)
     "Apply filters"]
    [:a
     (merge
       {:class (util/mc styles/bottom-button)}
       #?(:cljs {:on-click #(do
                              (dispatch [:wh.jobs.jobsboard.events/initialize-db])
                              (dispatch [:wh.search/search]))}
          :clj  {:href "?interaction=1"}))
     "Reset all filters"]]

   (let [jobs (<sub [::subs/side-jobs])]
     (when (and (<sub [:user/logged-in?]) (not-empty jobs))
       [side-cards/jobs {:jobs                  jobs
                         :jobs-loading?         (<sub [::subs/side-jobs-loading?])
                         :company?              (<sub [:user/company?])
                         :show-recommendations? true}]))])

(defn- view-types [view-type on-change]
  (let [types {:view/list  {:icon-name "list"
                            :class     styles/icon--list}
               :view/cards {:icon-name "board-rectangles"
                            :class     styles/icon--card}}]
    [:div (util/smc styles/view-types)
     (for [[type {:keys [icon-name class] :as _value}] types]
       (let [selected (= (name type) (name view-type))]
         ^{:key type}
         [:a
          (merge
            (util/smc styles/view-types__item)
            (on-change type))
          [icon icon-name
           :class (util/mc
                    class
                    styles/icon
                    [selected styles/icon--selected])]]))]))

(defn navigation [{:keys [view-type logged-in? on-list-type-change]}]
  [:div (util/smc styles/header)
   [buttons-page-navigation/buttons-jobsboard {:logged-in? logged-in?}]
   [view-types view-type on-list-type-change]])

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
     [:h2 (util/smc styles/jobs-section__title) "Promoted Jobs"]

     [:div {:class (jobs-container-class view-type)}
      (for [job jobs]
        ^{:key (:id job)}
        [job/job-card job {:logged-in?        logged-in?
                           :view-type         view-type
                           :user-has-applied? has-applied?
                           :user-is-company?  (not (nil? company-id))
                           :user-is-owner?    (or admin? (= company-id (:company-id job)))
                           :apply-source      "jobsboard-promoted-job"}])]]))
