(ns wh.common.application)

(defn approved? [{:keys [state] :as _application}]
  (boolean (some-> state
                   keyword
                   (= :approved))))