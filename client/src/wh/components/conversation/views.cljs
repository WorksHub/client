;; Common widgets for conversation.

(ns wh.components.conversation.views
  (:require
    [re-frame.core :refer [dispatch dispatch-sync]]
    [reagent.core :as reagent]
    [wh.common.re-frame-helpers :refer [merge-classes]]
    [wh.components.icons :refer [icon]]
    [wh.subs :refer [<sub]]))

(defn codi-message [& parts]
  (let [[options parts] (if (map? (first parts))
                          [(first parts) (next parts)]
                          [{} parts])]
    [:aside.animatable.codi-container
     {:class (merge-classes (:class options)
                            (when (:hidden? options)
                              "codi-container--hidden"))}
     [icon "codi"]
     (into [:div.conversation-element.codi
            (when-let [on-close (:on-close options)]
              [icon "close" :class "close"
               :on-click #(dispatch on-close)])]
           parts)]))

(defn error-message [& parts]
  [:aside.animatable.codi-container
   {:class (when (nil? (first parts)) "codi-container--hidden")}
   [icon "codi"]
   (into [:div.conversation-element.codi.conversation-element--error] parts)])

(defn button [caption on-click-event & {:as options}]
  [:div.animatable
   (let [caption-v (if (string? caption) [caption] caption)]
     (into
      [:button.conversation-button
       (merge options
              {:on-click (when on-click-event #(dispatch on-click-event))})]
      caption-v))])
