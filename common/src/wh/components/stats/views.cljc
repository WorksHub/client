(ns wh.components.stats.views
  (:require [wh.components.icons :refer [icon]]
            #?(:clj  [wh.components.stats.core :as stats-core]
               :cljs [wh.components.stats.loadable :as stats-loadable])))

(defn stats-item [{:keys [icon-name caption x-axis y-axis values total change]}]
  [:div.stats__item {:class (str "stats__" icon-name)}
   [:div.stats__icon
    [icon icon-name]]
   [:div.stats__caption caption]
   [:div.stats__value-line
    [:span.stats__value total]
    [:span.stats__increase change]]
   (let [props {:x-axis x-axis
                :y-axis y-axis
                :values values}]
     #?(:clj  [stats-core/chart props]
        :cljs [stats-loadable/chart props]))])
