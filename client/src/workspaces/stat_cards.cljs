(ns workspaces.stat-cards
  (:require [nubank.workspaces.card-types.react :as ct.react]
            [nubank.workspaces.core :as ws]
            [reagent.core :as r]
            [wh.components.stat-card :as stat-card]
            [wh.verticals :as verticals]))

(ws/defcard all-cards
  (let [vertical-idx (atom 0)]
    (ct.react/react-card
      vertical-idx
      (r/as-element
        [:a {:href     "#"
             :on-click #(if (>= (inc @vertical-idx) (count verticals/all-verticals))
                          (reset! vertical-idx 0)
                          (swap! vertical-idx inc))}
         [:div {:style {:display  "grid"
                        :grid-gap "10px"
                        :width    "100%"}}
          [stat-card/about-applications (nth (vec verticals/all-verticals) @vertical-idx)]
          [stat-card/about-salary-increase (nth (vec verticals/all-verticals) @vertical-idx)]]]))))

(defonce init (ws/mount))
