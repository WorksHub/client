(ns wh.common.fx.scroll
  (:require [re-frame.core :refer [reg-fx]]
            [wh.pages.core :refer [force-scroll-to-top!]]))

(defn scroll-to-top
  ([]
   (force-scroll-to-top!))
  ([_ & rest]
   (scroll-to-top)))

;;

(defn verticalPosition
  "distance between page top and element top"
  [elm]
  (let [page-top-to-viewport-top (.-pageYOffset js/window)
        viewport-top-to-elm-top  (-> elm
                                     (.getBoundingClientRect)
                                     (.-top))]
    (+ page-top-to-viewport-top viewport-top-to-elm-top)))

;;

(defn current-header-height
  "returns current header height, different values for different viewports"
  []
  (let [css-var-header-height "--header-height"]
    (-> js/document
        (.-documentElement)
        (js/getComputedStyle)
        (.getPropertyValue css-var-header-height)
        js/parseInt)))

;;

(defn scroll-into-view-effect
  [id]
  (when id
    (cond
      (string? id)
      (when-let [elm (.getElementById js/document id)]
        (.scrollTo js/window
                   #js {:top      (- (verticalPosition elm)
                                     (current-header-height))
                        :behavior "smooth"}))
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
