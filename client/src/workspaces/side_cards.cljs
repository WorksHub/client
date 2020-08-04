(ns workspaces.side-cards
  (:require [nubank.workspaces.card-types.react :as ct.react]
            [nubank.workspaces.core :as ws]
            [reagent.core :as r]
            [wh.components.side-card.side-card :as side-card]))

(defn element [name props & children]
  (apply js/React.createElement name (clj->js props) children))

(ws/defcard osi-how-it-works-card
  (ct.react/react-card
    (r/as-element [:div {:style {:border    "1px solid black"
                                 :max-width "280px"}}
                   [side-card/osi-how-it-works]])))

(ws/defcard improve-your-recommendations-card
   (ct.react/react-card
     (r/as-element [:div {:style {:background "#eee"
                                  :max-width "350px"
                                  :padding "16px"}}
                    [side-card/improve-your-recommendations true]])))

(defonce init (ws/mount))
