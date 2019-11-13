(ns wh.company.listing.views
  (:require
    #?(:cljs [wh.user.subs])
    [clojure.string :as str]
    [wh.common.data.company-profile :as data]
    [wh.common.specs.company :as company-spec]
    [wh.common.specs.tags :as tag-spec]
    [wh.common.text :as text]
    [wh.company.listing.db :as companies]
    [wh.company.listing.db :as listing]
    [wh.company.listing.events :as events]
    [wh.company.listing.subs :as subs]
    [wh.company.profile.views :as company]
    [wh.components.common :refer [link wrap-img img base-img]]
    [wh.components.forms :as forms]
    [wh.components.icons :refer [icon]]
    [wh.components.pagination :as pagination]
    [wh.components.tag :as tag]
    [wh.interop :as interop]
    [wh.interop.forms :as interop-forms]
    [wh.pages.util :as putil]
    [wh.re-frame :as r]
    [wh.re-frame.events :refer [dispatch dispatch-sync]]
    [wh.re-frame.subs :refer [<sub]]
    [wh.util :as util]))

(defn company-card
  [{:keys [logo id name slug tags size location description profile-enabled
           total-published-job-count total-published-issue-count] :as _company}
   & [{:keys [view-jobs-link?]
       :or {view-jobs-link? true}}]]
  [:section.companies__company.company-card
   [:div.companies__company__container
    [:div.company-profile__logo.is-hidden-mobile
     (let [wrapped-logo (wrap-img img logo {:w 60 :h 60})]
       (if profile-enabled
         [link wrapped-logo  :company :slug slug]
         wrapped-logo))]
    [:div.companies__company__info-container
     [:div.company-profile__name
      (let [header [:div.is-flex
                    [:div.company-profile__logo.is-hidden-desktop
                     (wrap-img img logo {:w 36 :h 36})]
                    [:h2 {:class (when (and total-published-job-count (pos? total-published-job-count)) "truncate")} name]]]
        (if profile-enabled
          [link header :company :slug slug]
          header))]
     [:ul.companies__company__info-strip
      (when size
        [:li [:div [icon "people"] size]])
      (when location
        [:li [:div [icon "location"] location]])]
     [:div.companies__company__description
      [:p description]]
     [:div.companies__company__tags
      [tag/tag-list (cond-> (vec tags)
                            (and total-published-issue-count (pos? total-published-issue-count))
                            (conj {:icon "pr" :id id :type :icon :label total-published-issue-count}))]]]]
   (when (and view-jobs-link? total-published-job-count (pos? total-published-job-count))
     [link
      [:div.companies__company__job-count
       [:img {:src "/cursors/cursor-2.svg"}]
       [:span (str "View " total-published-job-count " " (text/pluralize total-published-job-count "job"))]]
      :company-jobs :slug slug])])

(defn page
  []
  (let [result       (<sub [::subs/companies])
        companies    (or (:companies result)
                         (map (partial hash-map :id) (range 10)))
        query-params (<sub [:wh/query-params])
        loading?     (<sub [::subs/loading?])]
    [:div
     [:div.main.companies
      [:h1 "Companies using WorksHub"]
      [:div.split-content
       [:div.companies__main.split-content__main
        [:div.companies__filtering
         [forms/tags-field
          {:id                 listing/tag-field-id
           :solo?              true
           :hide-error?        true
           :placeholder        "Search company tags, such as technologies (java, scala, etc) or benefits (remote, pension, etc)"
           :init-from-js-tags? true
           :on-change          (interop-forms/add-tag-value-to-url :tag ::events/on-tag-change)}]
         [forms/labelled-checkbox
          {:id          (str listing/tag-field-id "__live-jobs")
           :value       (<sub [::subs/live-jobs-only?])
           :solo?       true
           :hide-error? true
           :label       "Only show hiring companies"
           :on-change   (interop-forms/add-check-value-to-url :jobs)}]]
        [:div.companies__sorting
         [:div {:class (util/merge-classes
                         "companies__count"
                         (when loading? "skeleton"))}
          [:span (<sub [::subs/companies-count-str])]]
         [:div.companies__sorting__dropdown-container
          [:span.companies__sorting__dropdown-label "Sort by:"]
          [forms/select-field
           {:solo?     true
            :value     (<sub [::subs/sorting-by])
            :class     "companies__sorting__dropdown"
            :options   (<sub [::subs/sorting-options])
            :on-change (interop-forms/add-select-value-to-url :sort (<sub [::subs/sorting-options]))}]]]
        (doall
          (for [company companies]
            ^{:key (:id company)}
            [company-card company]))
        (when (and (not-empty companies) (> (<sub [::subs/total-number-of-results]) companies/page-size))
          [pagination/pagination
           (<sub [::subs/current-page])
           (<sub [::subs/pagination])
           :companies
           query-params])]
       [:div.companies__side.split-content__side.is-hidden-mobile
        [company/company-cta false]]]]]))
