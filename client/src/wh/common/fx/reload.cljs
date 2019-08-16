(ns wh.common.fx.reload
  (:require
    [re-frame.core :refer [reg-fx]]))

(defn reload-effect
  [reload?]
  (when reload?
    (.reload js/location)))

(reg-fx :reload reload-effect)
