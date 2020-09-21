(ns workspaces.profile-cards
  (:require [nubank.workspaces.card-types.react :as ct.react]
            [nubank.workspaces.core :as ws]
            [reagent.core :as r]
            [wh.logged-in.profile.components :as components]))

(ws/defcard stats-grid
  (ct.react/react-card
    (r/as-element
      [:div
       {:style {:width "100vw"}}
       [components/stats-grid
        [components/stat-container
         {:title "Title"
          :text  "Text"
          :image "Image"
          :key   :foo}]
        [components/stat-container
         {:title "Title"
          :text  "Text"
          :image "Image"
          :key   :bar}]
        [components/stat-container
         {:title "Title"
          :text  [:a {:href "#"} "Link"]
          :image "Image"
          :key   :baz}]
        [components/stat-container
         {:title "Title"
          :text  [:a {:href "#"} "Link"]
          :image "Image"
          :key   :qux}]]])))

(defonce init (ws/mount))
