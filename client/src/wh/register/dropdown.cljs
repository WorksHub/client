(ns wh.register.dropdown
  (:require [re-frame.core :refer [dispatch dispatch-sync]]
            [reagent.core :as r]
            [wh.common.keycodes :refer [codes]]
            [wh.subs :refer [<sub]]))

(defn submit-focused-item
  [event dropdown-sub set-dropdown-ev]
  (let [index (js/parseInt (.-index (.-dataset (.-target event))))
        items (vec (<sub dropdown-sub))
        id (:id (get (vec items) index))]
    (dispatch-sync (conj set-dropdown-ev id))))

(defn focus
  [dropdown-items item]
  ;; first set tabIndex all to -1
  (let [counter (atom 0)]
    (while (< @counter (.-length dropdown-items))
      (do
        (set! (.-tabIndex (.item dropdown-items @counter)) "-1")
        (swap! counter inc)))
    (set! (.-tabIndex item) "0")
    (.focus item)))

(defn focus-item
  [direction dropdown-items]
  (let [input-active? (= "dropdown__input" (.-id (.-activeElement js/document)))]
    (if input-active?
      (focus dropdown-items (.item dropdown-items 0))
      (let [index (js/parseInt (.-index (.-dataset (.-activeElement js/document))))]
        (cond
          (and (= direction :down) (<= index 3)) (focus dropdown-items (.item dropdown-items (inc index)))
          (and (= direction :up) (> index 0))    (focus dropdown-items (.item dropdown-items (dec index))))))))

;; handler for the input event, to focus 
(defn keydownhandler
  [dropdown-items dropdown-sub set-dropdown-ev set-input-ev event]
  (let [keycode (.-keyCode event)]
    (cond
      (= keycode (:down codes))  (focus-item :down dropdown-items)
      (= keycode (:up codes))    (focus-item :up dropdown-items)
      (= keycode (:enter codes)) (submit-focused-item event dropdown-sub set-dropdown-ev))))

(defn dropdown
  [items dropdown-sub set-dropdown-ev set-input-ev]
  (let []
    (r/create-class
     {:display-name "dropdown"
      :reagent-render (fn [items set-dropdown-ev set-input-ev]
                        (into [:ul {:id "dropdown__options"}]
                              (map (fn [{:keys [id label] :as item}]
                                     (let [data-index (.indexOf items item)
                                           data-focused false]
                                       [:li {:tabIndex "-1"
                                             :data-index data-index
                                             :on-click #(when set-dropdown-ev
                                                          (dispatch (conj set-dropdown-ev id)))}
                                        label]))
                                   items)))
      :component-did-mount (fn [this]
                             (let [input (.getElementById js/document "dropdown__input")
                                   dropdown (.getElementById js/document "dropdown__options")
                                   dropdown-items (.querySelectorAll dropdown "li")
                                   length (.-length dropdown-items)
                                   counter (atom 0)]
                               (do
                                 ;; first set event listener for the input
                                 (.addEventListener input "keydown" (partial keydownhandler dropdown-items dropdown-sub set-dropdown-ev set-input-ev))

                                 ;; then all the listeners for each item
                                 (while (< @counter length)
                                   (do
                                     (.addEventListener (.item dropdown-items @counter) "keydown" (partial keydownhandler dropdown-items dropdown-sub set-dropdown-ev set-input-ev))
                                     (swap! counter inc))))))})))
