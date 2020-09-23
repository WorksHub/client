(ns wh.logged-in.profile.components.contributions
  (:require [wh.styles.profile :as styles]
            [wh.util :as util]))

(defn grid [week]
  [:div (util/smc styles/grid__week)
   (for [{:keys [weekday color] :as _day} week]
     ^{:key weekday}
     [:div
      (merge
        (util/smc styles/grid__day)
        (when color {:style {:background-color color}}))])])

(defn legend-days []
  [:div (util/smc styles/legend__days)
   [:div (util/smc styles/legend__days__day) "Mon"]
   [:div (util/smc styles/legend__days__day) "Wed"]
   [:div (util/smc styles/legend__days__day) "Fri"]])

(defn legend-months [months]
  [:div (util/smc styles/legend__months)
   (for [month months]
     ^{:key month}
     [:div month])])

(defn contributions-grid [contributions months]
  [:div (util/smc styles/grid-cell-2-rows
                  styles/stat-container
                  styles/stat-container--big)
   [:div (util/smc styles/stat-container__title)
    "Last 4 months productivity"]
   [:div (util/smc styles/github-contributions)
    [legend-months months]

    [legend-days]

    [:div (util/smc styles/grid)
     (for [week contributions]
       ^{:key (-> week first :date)}
       [grid week])]]])
