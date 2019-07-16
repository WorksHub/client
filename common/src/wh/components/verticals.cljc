(ns wh.components.verticals
  (:require
    [re-frame.core :refer [dispatch]]
    [wh.components.icons :refer [icon]]
    [wh.components.navbar :as nav-common]
    [wh.util :as util]))

(defn verticals-pod
  [{:keys [toggleable? on-verticals off-verticals toggle-event]}]
  [:div
   {:class "verticals-pod-wrapper"}
   [:div.wh-formx [:h2.is-hidden-desktop "Hubs"]]
   [:div.pod
    {:class "verticals-pod"}
    [:span "Currently visible in: " [:i "(click to change)"] ]
    [:div
     {:class (util/merge-classes
              "verticals-pod__verticals"
              "verticals-pod__verticals--on")}
     (doall (for [vertical on-verticals]
              ^{:key vertical}
              [:div
               (when toggleable?
                 {:class "verticals-pod__vertical-toggle"
                  :on-click #(dispatch (conj toggle-event vertical))})
               [icon vertical] (nav-common/logo-title vertical)]))]
    [:hr]
    [:div
     {:class (util/merge-classes
              "verticals-pod__verticals"
              "verticals-pod__verticals--off")}
     (doall (for [vertical off-verticals]
              ^{:key vertical}
              [:div
               (merge {:class (if toggleable?
                                "verticals-pod__vertical-toggle"
                                "verticals-pod__vertical--disabled")}
                      (when toggleable?
                        {:on-click #(dispatch (conj toggle-event vertical))}))
               [icon vertical] (nav-common/logo-title vertical)]))]]])
