(ns wh.components.loader.views)

(defn loader []
  [:svg.loader.loader-1 [:use {:xlinkHref "#loader-1"}]])

(defn loader-cover
  "Show a centered loader on top of child if pred is true"
  [pred child]
  [:div.load-wrap
    child
    (when pred
      [:div.loader-container
       (loader)])])
