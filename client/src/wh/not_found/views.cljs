(ns wh.not-found.views
  (:require
    [wh.components.error.views :refer [not-found]]))

(defn page []
  [:div.dashboard
    [:div.main.main--center-content
      [not-found]]])
