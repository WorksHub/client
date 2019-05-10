(ns wh.common.fx.scroll
  (:require
    [re-frame.core :refer [reg-fx]]
    [wh.pages.core :refer [force-scroll-to-top!]]))

(defn scroll-into-view-effect
  [id]
  (letfn [(visible? [el]
            (and (> (.-offsetWidth el) 0)
                 (> (.-offsetHeight el) 0)))]
    (when id
      (cond
        (string? id)
        (when-let [el (.getElementById js/document id)]
          (when (visible? el)
            (.scrollIntoView el #js {:behavior "smooth"})))
        (sequential? id)
        (doseq [id' id]
          (scroll-into-view-effect id'))))))

(reg-fx :scroll-into-view scroll-into-view-effect)

(defn scroll-to-top
  [_]
  (force-scroll-to-top!))

(reg-fx :scroll-to-top scroll-to-top)
