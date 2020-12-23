(ns wh.promotions.preview.views
  (:require [reagent.core :as r]
            [wh.promotions.preview.events]
            [wh.promotions.preview.subs :as subs]
            [wh.re-frame.subs :refer [<sub]]
            [wh.styles.promotions :as styles]
            [wh.util :as util]))

(defn promotion
  [{:keys [id start-date object-type object-id] :as promotion}]
  (r/with-let [expanded? (r/atom false)]
    [:li {:class (util/mc styles/preview__promotion) :key id}
     [:div (util/smc styles/header)
      [:p {:class styles/date}
       (str (.toLocaleString (js/Date. start-date)) " | " object-type " (" object-id ")")]
      [:a {:class    styles/show-hide
           :href     "#"
           :on-click #(swap! expanded? not)}
       (if @expanded? "Hide" "Show")]]

     (when @expanded?
       [:div
        [:pre (str promotion)]])]))

(defn page []
  (let [promotions (<sub [::subs/promotions])]
    [:div.main
     [:h1 "Promotions"]

     [:ul
      (for [data promotions]
        [promotion data])]]))
