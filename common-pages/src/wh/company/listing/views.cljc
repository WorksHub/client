(ns wh.company.listing.views
  (:require
    #?(:cljs [wh.user.subs])
    [clojure.string :as str]
    [wh.common.data.company-profile :as data]
    [wh.common.specs.company :as company-spec]
    [wh.common.specs.tags :as tag-spec]
    [wh.common.text :as text]
    [wh.company.listing.db :as companies]
    [wh.company.listing.events :as events]
    [wh.company.listing.subs :as subs]
    [wh.company.profile.views :as company]
    [wh.components.common :refer [link wrap-img img base-img]]
    [wh.components.icons :refer [icon]]
    [wh.components.pagination :as pagination]
    [wh.components.tag :as tag]
    [wh.pages.util :as putil]
    [wh.re-frame :as r]
    [wh.re-frame.events :refer [dispatch dispatch-sync]]
    [wh.re-frame.subs :refer [<sub]]
    [wh.util :as util]))

(defn company-card
  [{:keys [logo id name slug tags size location description-html profile-enabled
           total-published-job-count total-published-issue-count] :as _company}]
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
      [putil/html description-html]]
     [:div.companies__company__tags
      [tag/tag-list (cond-> (vec tags)
                            (and total-published-issue-count (pos? total-published-issue-count))
                            (conj {:icon "pr" :id id :type :icon :label total-published-issue-count}))]]]]
   ;; TODO we disable this (for now) when profile disabled because we have no where to send users!
   (when (and total-published-job-count (pos? total-published-job-count) profile-enabled)
     [link
      [:div.companies__company__job-count
       [:img {:src "/cursors/cursor-2.svg"}]
       [:span (str "View " total-published-job-count " " (text/pluralize total-published-job-count "job"))]]
      :company :slug slug])]) ;; TODO link to jobs?

(defn page
  []
  (let [admin?          (<sub [:user/admin?])
        current-page    (<sub [::subs/current-page])
        result          (<sub [::subs/companies])
        {:keys [total]} (:pagination result)
        companies       (or (:companies result)
                            (map (partial hash-map :id) (range 10)))
        query-params (<sub [:wh/query-params])]
    [:div
     [:div.main.companies
      [:h1 "Companies using WorksHub"]
      [:div.split-content
       [:div.companies__main.split-content__main
        (doall
          (for [company companies]
            ^{:key (:id company)}
            [company-card company]))
        (when (and (not-empty companies) (> total companies/page-limit))
          [pagination/pagination
           current-page
           (pagination/generate-pagination
             current-page
             (int (#?(:cljs js/Math.ceil :clj Math/ceil) (/ total companies/page-limit))))
           :companies query-params])]
       [:div.companies__side.split-content__side.is-hidden-mobile
        [company/company-cta false]]]]]))
