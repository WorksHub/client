(ns wh.common.application)

(defn approved? [{:keys [state] :as _application}]
  (boolean (some-> state
                   keyword
                   (= :approved))))

(defn hubspot-deal-id [a] (get-in a [:hubspot :id]))
(defn state [a] (:state a))
