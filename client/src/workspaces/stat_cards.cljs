(ns workspaces.stat-cards
  (:require [nubank.workspaces.card-types.react :as ct.react]
            [nubank.workspaces.core :as ws]
            [reagent.core :as r]
            [wh.components.stat-card :as stat-card]))

(ws/defcard all-cards
  (ct.react/react-card
    (r/as-element [:div {:style {:display "grid"
                                 :grid-gap "10px"
                                 :width "600px"}}
                   [stat-card/about-applications]
                   [stat-card/about-open-source]
                   [stat-card/about-salary-increase]])))

(defonce init (ws/mount))

