(ns wh.common.fx.scroll
  (:require [re-frame.core :refer [reg-fx]]
            [wh.pages.core :refer [force-scroll-to-top!]]))

(defn scroll-to-top
  ([]
   (force-scroll-to-top!))
  ([_ & rest]
   (scroll-to-top)))

(defn scroll-into-view-effect
  [id]
  (when id
    (cond
      (string? id)
      (when-let [el (.getElementById js/document id)]
        (.scrollIntoView el #js {:behavior "smooth"}))
      (sequential? id)
      (doseq [id' id]
        (scroll-into-view-effect id'))
      (nil? id)
      (scroll-to-top))))

(reg-fx :scroll-to-top scroll-to-top)
(reg-fx :scroll-into-view scroll-into-view-effect)
