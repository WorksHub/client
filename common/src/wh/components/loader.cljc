(ns wh.components.loader
  (:require [wh.util :as util]))

(defn loader
  ([]
   (loader nil))
  ([class]
   [:svg
    (util/smc "loader" "loader-1" class)
    [:use {#?(:clj :xlink:href) #?(:cljs :xlink-href) "#loader-1"}]]))

(defn loader-cover
  "Show a centered loader on top of child if pred is true"
  [pred child]
  [:div.load-wrap
   child
   (when pred
     [:div.loader-container
      (loader)])])

(defn loader-full-page []
  [:div.loader-wrapper
   [loader]])

