(ns wh.components.loader)

(defn loader []
  [:svg.loader.loader-1
   [:use {#?(:clj :xlink:href) #?(:cljs :xlink-href) "#loader-1"}]])

(defn loader-cover
  "Show a centered loader on top of child if pred is true"
  [pred child]
  [:div.load-wrap
   child
   (when pred
     [:div.loader-container
      (loader)])])
