(ns wh.homepage.views
  (:require
    [wh.components.www-homepage :as www]
    [wh.landing.views :as landing]
    [wh.subs :refer [<sub]]))

(defn page []
  (cond
    (= "www" (<sub [:wh.subs/vertical]))
    (www/homepage (<sub [:wh.subs/page-params]))

    true
    (landing/page)))
