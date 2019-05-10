(ns wh.components.stats.impl
  (:require
    [clojure.walk :refer [prewalk]]
    [thi.ng.color.core :as col]
    [thi.ng.geom.svg.core :as svg]
    [thi.ng.geom.vector :as v]
    [thi.ng.geom.viz.core :as viz]
    [thi.ng.math.core :as m :refer [PI TWO_PI]]
    [wh.components.icons :refer [icon]]
    [wh.components.stats.views :as views]))

(defn export-viz
  [viz fill-id]
  [:svg {:viewBox "0 0 300 100"
         :preserveAspectRatio "none"
         :class "stats__chart"}
   (svg/defs (svg/linear-gradient
              fill-id
              {:x1 "50%"
               :y1 "100%"
               :x2 "50%"
               :y2 "0%"}
              [0 "#fafbfb"]
              [1 "#c7dadd"]))
   viz])

(defn viz-spec
  [x-axis y-axis values fill-id]
  {:x-axis (viz/linear-axis x-axis)
   :y-axis (viz/linear-axis y-axis)
   :data   [{:values  values
             :attribs {:fill (str "url(#" fill-id ")")}
             :layout  viz/svg-area-plot}
            {:values  values
             :attribs {:stroke "#8cb4bb" :stroke-width "3px" :stroke-linecap "round"}
             :layout  viz/svg-line-plot}]})

(defn muffle-react-warnings
  [component]
  (prewalk (fn [x]
             (if-not (vector? x)
               x
               (vec (mapcat #(if (seq? %) % [%]) x))))
           component))

(defn chart [x-axis y-axis values]
  (let [fill-id (str (gensym "bg"))]
    (-> (viz-spec x-axis y-axis values fill-id)
        (viz/svg-plot2d-cartesian)
        (export-viz fill-id)
        (muffle-react-warnings))))

(defn stats-item [{:keys [icon-name caption x-axis y-axis values total change]}]
  [:div.stats__item {:class (str "stats__" icon-name)}
   [:div.stats__icon
    [icon icon-name]]
   [:div.stats__caption caption]
   [:div.stats__value-line
    [:span.stats__value total]
    [:span.stats__increase change]]
   [chart x-axis y-axis values]])

(reset! views/stats-item-impl stats-item)
