(ns wh.common.fx.auth
  (:require
    [re-frame.core :refer [reg-fx dispatch]]))

(defn show-auth-popup-effect
  [{:keys [context redirect]}]
  (js/showAuthPopUp (name context) (pr-str redirect)))

(reg-fx :show-auth-popup show-auth-popup-effect)
