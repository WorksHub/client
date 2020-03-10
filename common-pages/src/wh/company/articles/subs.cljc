(ns wh.company.articles.subs
  (:require
    [re-frame.core :refer [reg-sub reg-sub-raw]]
    [wh.company.articles.db :as articles]
    [wh.company.listing.db :as listing]
    [wh.company.profile.db :as profile]
    [wh.components.pagination :as pagination]
    [wh.graphql-cache :as graphql]
    [wh.re-frame.subs :refer [<sub]]
    [wh.util :as util])
  (#?(:clj :require :cljs :require-macros)
    [wh.re-frame.subs :refer [reaction]]))

(reg-sub
  ::company-slug
  (fn [db _]
    (articles/company-slug db)))

(reg-sub
  ::page-number
  (fn [db _]
    (articles/page-number db)))

(reg-sub-raw
  ::company
  (fn [_ _]
    (reaction
      (let [slug   (<sub [:wh/page-param :slug])
            result (<sub [:graphql/result :company-card {:slug slug}])]
        (profile/->company (:company result))))))

(reg-sub-raw
  ::articles-raw
  (fn [db _]
    (reaction
      (let [slug        (<sub [::company-slug])
            page-number (<sub [::page-number])
            result      (<sub [:graphql/result :company-articles-page {:slug slug :page_size articles/page-size :page_number page-number}])]
        (:company result)))))

(reg-sub
  ::state
  (fn [db _]
    (let [params (articles/params db)
          query-name :company-articles-page]
      (graphql/state db query-name params))))

(reg-sub
  ::articles
  :<- [::state]
  :<- [::articles-raw]
  (fn [[state articles-raw] _]
    (if (= state :executing)
      (util/maps-with-id (/ articles/page-size 2))
      (get-in articles-raw [:blogs :blogs]))))

(reg-sub
  ::total-number-of-articles
  :<- [::articles-raw]
  (fn [articles-raw _]
    (or (get-in articles-raw [:blogs :pagination :total]) 0)))

(reg-sub
  ::number-of-pages
  :<- [::total-number-of-articles]
  (fn [total _]
    (pagination/number-of-pages articles/page-size total)))

(reg-sub
  ::name
  :<- [::company]
  (fn [company _]
    (:name company)))

(reg-sub
  ::company-card
  :<- [::company]
  (fn [company _]
    (listing/->company company)))
