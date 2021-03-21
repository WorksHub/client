(ns wh.landing.views
  (:require [wh.components.www-homepage :as www]
            [wh.re-frame.subs :refer [<sub]]))

(defn page []
  (www/homepage (<sub [:wh/page-params])))
