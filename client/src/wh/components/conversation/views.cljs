;; Common widgets for conversation.

(ns wh.components.conversation.views
  (:require
    [re-frame.core :refer [dispatch dispatch-sync]]
    [reagent.core :as reagent]
    [wh.components.icons :refer [icon]]
    [wh.subs :refer [<sub]]
    [wh.util :refer [merge-classes]]))

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
              (when on-click-event
                {:on-click #(dispatch on-click-event)}))]
      caption-v))])
