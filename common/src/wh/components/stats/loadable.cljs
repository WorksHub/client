(ns wh.components.stats.loadable
  (:require [shadow.lazy :as lazy]
            [wh.re-frame :refer [atom]]))

(def chart-lazy (lazy/loadable wh.components.stats.core/chart))
(def default (fn [] [:div]))

(defn chart [_]
  (let [component (atom default)
        _promise  (-> (lazy/load chart-lazy)
                      (.then (fn [root-el]
                               (reset! component root-el))))]
    (fn [props]
      [@component props])))
