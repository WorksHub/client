(ns wh.company.profile.views
  (:require
    [wh.company.profile.events :as events]
    [wh.company.profile.subs :as subs]
    [wh.components.videos :as videos]
    [wh.re-frame.subs :refer [<sub]]))

(defn header []
  [:div.company-profile__header
   [:div.company-profile__logo
    [:img {:src (<sub [::subs/logo])}]]
   [:div.company-profile__name (<sub [::subs/name])]])

(defn videos
  []
  [:section.company-profile__videos
   [:h2.header "Videos"]
   [:ul.company-profile__videos__list
    [videos/videos {:videos        (<sub [::subs/videos])
                    :can-add-edit? false
                    :error         (<sub [::subs/video-error])
                    :delete-event  [::events/delete-video]
                    :add-event     [::events/add-video]
                    :update-event  [::events/update-video]}]]])

(defn page []
  [:div.main.company-profile
   [header]
   [:div.company-profile__main.split-content__main
    [videos]]
   [:div.company-profile__side.split-content__side
    ]])
