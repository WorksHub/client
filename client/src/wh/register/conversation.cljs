(ns wh.register.conversation
  (:require
    [re-frame.core :refer [dispatch dispatch-sync]]
    [reagent.core :as r]
    [wh.common.keycodes :as kc]
    [wh.components.conversation.views :refer [button]]
    [wh.components.icons :refer [icon]]
    [wh.subs :refer [<sub]]))

(defn next-button []
  [button "Next »" [:register/advance]])

(defn github-button []
  [:button.github-button
   {:on-click #(dispatch [:github/call])}
   [icon "github" :class "github-button__icon"]
   [:span.github-button__caption "Connect to" [:br] "GitHub"]])

(defn input-with-suggestions
  [& {:keys [input-sub error-sub dropdown-sub
             set-input-ev set-dropdown-ev pick-ev
             on-close placeholder label]}]
  (r/with-let [selected (r/atom nil)]
    (let [items       (and dropdown-sub (<sub dropdown-sub))
          last-idx    (- (count items) 1)
          submit-fn   #(do (.preventDefault %)
                           (if (nil? @selected)
                             (when pick-ev (dispatch pick-ev))
                             (.click (js/document.querySelector (str ".input-with-suggestions li[tabindex='" @selected "']")))))
          on-key-down #(condp = (.-which %)
                         kc/up   (when (seq items)
                                   (.preventDefault %)
                                   (if (nil? @selected)
                                     (reset! selected last-idx)
                                     (if (< @selected 1)
                                       (reset! selected nil)
                                       (swap! selected dec))))
                         kc/down (when (seq items)
                                   (.preventDefault %)
                                   (if (nil? @selected)
                                     (reset! selected 0)
                                     (if (>= @selected last-idx)
                                       (reset! selected nil)
                                       (swap! selected inc))))
                         nil)]
     [:div.animatable.input-with-suggestions
      [:form.conversation-element.user
       {:on-submit submit-fn
        :on-key-down on-key-down}
       [:div.input-with-suggestions__container
        [:label.label {:for label} label]
        [:input
         (merge {:type :text
                 :auto-focus true
                 :id label
                 :auto-complete "off"}
                (when placeholder
                  {:placeholder placeholder})
                (when input-sub
                  {:value (<sub input-sub)})
                (when set-input-ev
                  {:on-change #(do (reset! selected nil)
                                   (dispatch-sync (conj set-input-ev (-> % .-target .-value))))}))]
        (when on-close
          [icon "close" :class :close :on-click #(dispatch on-close)])]
       (when-let [error (and error-sub (<sub error-sub))]
         [:ul [:li error]])
       (when (seq items)
         (into [:ul]
               (map-indexed (fn [i {:keys [id label]}]
                              [:li {:on-click #(when set-dropdown-ev
                                                 (dispatch (conj set-dropdown-ev id)))
                                    :tab-index i
                                    :class (when (= i @selected) "hover")}
                               label])
                    items)))]])))
