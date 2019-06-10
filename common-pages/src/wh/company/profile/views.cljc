(ns wh.company.profile.views
  (:require
    [wh.re-frame.subs :refer [<sub]]
    [wh.company.profile.events]
    [wh.company.profile.subs :as subs]))

(defn header []
  [:div.company-profile__header
   [:div.company-profile__logo
    [:img {:src (<sub [::subs/logo])}]]
   [:div.company-profile__name (<sub [::subs/name])]])

(defn page []
  [:div.main.company-profile
   [header]])
