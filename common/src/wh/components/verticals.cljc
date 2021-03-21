(ns wh.components.verticals
  (:require
    #?(:cljs [wh.components.forms.views :as views])
    [re-frame.core :refer [dispatch]]
    [wh.components.branding :as branding]
    [wh.components.icons :refer [icon]]
    [wh.util :as util]))


(defn vertical-line [status vertical toggle-event]
  (when toggle-event
    #?(:cljs (views/labelled-checkbox
               status
               (merge {:label       [:div {:class "verticals-pod__vertical-toggle"}
                                     [icon vertical] (branding/vertical-title vertical {:size :small})]
                       :label-class "verticals-pod__vertical-toggle-wrapper"}
                      (when toggle-event {:on-change (conj toggle-event vertical)}))))))

(defn verticals-pod
  [{:keys [on-verticals off-verticals toggle-event]}]
  [:div
   {:class "verticals-pod-wrapper"}
   [:div.wh-formx [:h2.is-hidden-desktop "Hubs"]]
   [:div.pod
    {:class "verticals-pod"}
    [:span "Will be posted on: "]
    [:div
     {:class (util/merge-classes
               "verticals-pod__verticals"
               "verticals-pod__verticals--on")}
     (doall (for [vertical on-verticals]
              ^{:key vertical}
              [vertical-line true vertical toggle-event]))]
    [:div
     {:class (util/merge-classes
               "verticals-pod__verticals"
               "verticals-pod__verticals--off")}
     (doall (for [vertical off-verticals]
              ^{:key vertical}
              [vertical-line false vertical toggle-event]))]]])
