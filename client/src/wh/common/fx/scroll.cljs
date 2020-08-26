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

(defn scroll-to-bottom
  [elm-id]
  (let [elm (.getElementById js/document elm-id)]
    (set! (.-scrollTop elm) (- (.-scrollHeight elm) (.-clientHeight elm)))))

(defn scroll-to-bottom-with-delay
  "timeout is used because there is no guarantee that elements (which make scroll to appear) have rendered
  by the time this function is called (because of async re-frame nature), so we want to wait a little bit.
  I use 250 so it doesn't feel broken."
  [elm-id]
  (js/setTimeout #(scroll-to-bottom elm-id) 250))

(reg-fx :scroll-to-top scroll-to-top)
(reg-fx :scroll-into-view scroll-into-view-effect)
(reg-fx :scroll-to-bottom scroll-to-bottom-with-delay)
