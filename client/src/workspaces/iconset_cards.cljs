(ns workspaces.iconset-cards
  (:require [nubank.workspaces.card-types.react :as ct.react]
            [nubank.workspaces.core :as ws]
            [nubank.workspaces.model :as wsm]
            [reagent.core :as r]
            [wh.components.icons :as icons]
            [wh.styles.iconsset :as style]))

(defn get-icons []
  (->> (array-seq (js/document.querySelectorAll ".svg-container"))
       (map (fn [elm] (. elm -innerHTML))
       (apply str)
       (re-seq #"symbol id=\"(.+?)\"\s")
       (map second)))

(defn all-icons []
  (let [icon-names (r/atom [])]
    (r/create-class
      {:component-did-mount (fn [] (reset! icon-names (get-icons)))
       :reagent-render      (fn []
                              [:div {:class style/page}
                               (for [icon-name @icon-names]
                                 [:div {:class style/icon__wrapper}
                                  [icons/icon icon-name :class style/icon]
                                  [:span icon-name]])])})))

(ws/defcard all-icons
   {::wsm/card-width 12
    ::wsm/card-height 20}
   (ct.react/react-card
     (r/as-element [all-icons])))

(defonce init (ws/mount))

