(ns wh.register.conversation
  (:require
    [re-frame.core :refer [dispatch dispatch-sync]]
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
  [:div.animatable.input-with-suggestions
   [:form.conversation-element.user
    {:on-submit #(do
                   (.preventDefault %)
                   (when pick-ev
                     (dispatch pick-ev)))}
    [:div.input-with-suggestions__container
     [:label.label {:for label} label]
     [:input
      (merge {:type :text
              :auto-focus true
              :id label}
             (when placeholder
               {:placeholder placeholder})
             (when input-sub
               {:value (<sub input-sub)})
             (when set-input-ev
               {:on-change #(dispatch-sync (conj set-input-ev (-> % .-target .-value)))}))]
     (when on-close
       [icon "close" :class :close :on-click #(dispatch on-close)])]
    (when-let [error (and error-sub (<sub error-sub))]
      [:ul [:li error]])
    (let [items (and dropdown-sub (<sub dropdown-sub))]
      (when (seq items)
        (into [:ul]
              (map (fn [{:keys [id label]}]
                     [:li {:on-click #(when set-dropdown-ev
                                        (dispatch (conj set-dropdown-ev id)))}
                      label])
                   items))))]])
