(ns wh.components.ellipsis.views
  (:require [clojure.string :as str]
            [reagent.core :as r]))

(defn cap-text
  "Use a 2D canvas context to measure our string, capping it to a given set of dimensions"
  [txt w h ctx parent clearence]
  (when-not @ctx
    (reset! ctx (.getContext (.createElement js/document "canvas") "2d")))
  (let [ps    (.getComputedStyle js/window parent)
        rows  (int (/ h (js/parseInt (str/replace (.-lineHeight ps) #"px" ""))))
        w     (- (* rows w) clearence)
        get-w #(.-width (.measureText @ctx %))]
    (set! (.-font @ctx) (str (.-fontSize ps) " " (.-fontFamily ps)))
    (when (> (get-w txt) w)
      (loop [remaining-strs (butlast (str/split txt #"\W"))]
        (when (not-empty remaining-strs)
          (let [trying (str/join " " remaining-strs)]
            (if (> (get-w trying) w)
              (recur (butlast remaining-strs))
              (str trying " ..."))))))))

(defn ellipsis
  [text & [{:keys [vcenter?]}]]
  (let [clearence    40 ;;px
        id           (name (gensym))
        id-child     (str id "-child")
        measurer     (atom nil)
        parent       #(.getElementById  js/document id)
        child        #(.getElementById  js/document id-child)
        last-w       (atom 0)]
    (r/create-class
     {:component-did-update
      (fn [this]
        (when (not= @last-w (.-offsetWidth (parent)))
          (r/force-update this)))
      :component-did-mount
      (fn [this]
        (when (not= @last-w (.-offsetWidth (parent)))
          (r/force-update this)))
      :reagent-render
      (fn [text]
        (let [capped-text
              (when (child)
                (let [w (.-offsetWidth (parent))]
                  (reset! last-w w)
                  (cap-text text w (.-offsetHeight (parent)) measurer (child) clearence)))]
          [:div.ellipsis-container
           {:id id
            :class (when vcenter? "ellipsis-container--vcenter")}
           [:div.ellipsis-content
            {:id id-child}
            (or capped-text text)]]))})))
