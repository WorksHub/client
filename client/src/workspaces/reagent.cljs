(ns workspaces.reagent
  (:require [nubank.workspaces.card-types.react :as ct.react]
            [reagent.core :as r]))

(defn reagent-card [c]
  (ct.react/react-card
    (r/as-element c)))
