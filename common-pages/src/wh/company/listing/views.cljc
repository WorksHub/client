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
    [wh.components.common :refer [link wrap-img img base-img]]
    [wh.components.icons :refer [icon]]
    [wh.components.tag :as tag]
    [wh.re-frame :as r]
    [wh.re-frame.events :refer [dispatch dispatch-sync]]
    [wh.re-frame.subs :refer [<sub]]
    [wh.util :as util]))

(defn page
  []
  (let [admin? (<sub [:user/admin?])]
    [:div
     [:div.main.companies
      [:h1 "Companies using WorksHub"]
      [:div.split-content
       [:div.companies__main.split-content__main]
       [:div.companies__side.split-content__side.is-hidden-mobile]]]]))
