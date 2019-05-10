(ns wh.components.get-started.views
  (:require
    [re-frame.core :refer [dispatch dispatch-sync]]
    [wh.components.common :refer [get-started-banner-template link]]
    [wh.components.icons :refer [icon]]
    [wh.pages.core :refer [load-and-dispatch]]
    [wh.subs :refer [<sub]]))

(defn get-started-buttons
  [[event-type data]]
  (let [event-data (merge {:type event-type} data)]
    [:div.register-cta-buttons
     [:button.button.button--medium
      {:on-click #(load-and-dispatch [:login [:github/call event-data]])}
      [icon "github"]
      "Start with GitHub"]
     [:button.button.button--medium
      {:on-click #(dispatch [:register/get-started event-data])}
      "Get Started"]]))

(defn get-started-banner [event-data]
  (get-started-banner-template (partial get-started-buttons event-data)))
