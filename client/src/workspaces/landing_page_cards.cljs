(ns workspaces.landing-page-cards
  (:require [nubank.workspaces.card-types.react :as ct.react]
            [nubank.workspaces.core :as ws]
            [nubank.workspaces.model :as wsm]
            [reagent.core :as r]
            [wh.components.attract-card :as attract]
            [wh.verticals :as verticals]))

(ws/defcard attract-intro-card
  {::wsm/card-width 4 ::wsm/card-height 8}
  (let [vertical-idx (atom 0)]
    (ct.react/react-card
      vertical-idx
      (r/as-element
        [:a {:href     "#"
             :on-click #(if (>= (inc @vertical-idx) (count verticals/all-verticals))
                          (reset! vertical-idx 0)
                          (swap! vertical-idx inc))}
         [attract/intro (nth (vec verticals/all-verticals) @vertical-idx)]]))))

(ws/defcard attract-contribute-card
  {::wsm/card-width 4::wsm/card-height 8}
  (ct.react/react-card
    (r/as-element
      [attract/contribute false])))

(defonce init (ws/mount))
