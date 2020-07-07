(ns workspaces.side-cards
  (:require [nubank.workspaces.card-types.react :as ct.react]
            [nubank.workspaces.core :as ws]
            [reagent.core :as r]
            [wh.components.side-card.side-card :as side-card]))

(defn element [name props & children]
  (apply js/React.createElement name (clj->js props) children))

(ws/defcard recent-jobs
  (ct.react/react-card
    (r/as-element (let [job {:title   "Clojurescript developer"
                             :slug    "random-slug"
                             :company {:name "WorksHub" :logo "https://source.unsplash.com/100x100/?logo" :slug "workshub"}}]
                    [side-card/recent-jobs (repeat 3 job)]))))

(ws/defcard osi-how-it-works-card
  (ct.react/react-card
    (r/as-element [:div {:style {:border    "1px solid black"
                                 :max-width "280px"}}
                   [side-card/osi-how-it-works]])))

(defonce init (ws/mount))
