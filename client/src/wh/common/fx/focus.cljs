(ns wh.common.fx.focus
  (:require
    [re-frame.core :refer [reg-fx dispatch]]))

(defn focus-effect
  [id]
  (when-let [el (.querySelector js/document id)]
    (.focus el)))

(reg-fx :focus focus-effect)