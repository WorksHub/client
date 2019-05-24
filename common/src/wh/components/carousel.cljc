(ns wh.components.carousel
  (:require
    #?(:cljs [reagent.core :as r])
    [wh.components.icons :refer [icon]]
    [wh.util :as util]))

(defn pips
  [n active-n on-click]
  [:div.carousel__pips-wrapper
   [:div.carousel__pips
    (for [i (range n)]
      ^{:key i}
      [:div
       (merge {:class (util/merge-classes "carousel-pip"
                                          "carousel-pip--clickable"
                                          (when (= i active-n) "carousel-pip--active"))}
              (when on-click
                {:on-click #(on-click i)}))
       (icon "circle")])]])

(defn carousel
  [items]
  (let [num-items   (count items)
        active-item (#?(:clj atom :cljs r/atom) 0)
        rotate      #?(:clj nil :cljs (.setInterval js/window (fn [] (swap! active-item #(mod (inc %) num-items))) 5000))
        on-click    #?(:clj nil :cljs #(do (.clearInterval js/window rotate)
                                           (reset! active-item %)))]
    (fn [items]
      [:div.carousel
       (doall
        (map-indexed
         (fn [i item]
           ^{:key (str i)}
           [:div {:class (util/merge-classes "carousel-item"
                                             (when (= @active-item i) "carousel-item--active"))} item])
         items))

       (pips num-items @active-item on-click)
       ;; poor man's analogue of componentDidMount() when SSR-ing
       #?(:clj [:script "enableCarousel(document.currentScript.parentNode)"])])))
