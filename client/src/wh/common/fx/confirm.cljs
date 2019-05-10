(ns wh.common.fx.confirm
  (:require
    [re-frame.core :refer [reg-fx dispatch]]))

(defn confirm-effect
  [{:keys [message on-ok on-cancel]}]
  (if (.confirm js/window message)
    (when on-ok (dispatch on-ok))
    (when on-cancel (dispatch on-cancel))))

(reg-fx :confirm confirm-effect)
