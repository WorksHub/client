(ns wh.components.error.views
  (:require [re-frame.core :refer [dispatch]]
            [wh.components.error.subs :as subs]
            [wh.components.icons :refer [icon]]
            [wh.subs :refer [<sub]]))

(defn global-status-box []
  (let [type (<sub [::subs/type])]
    (when-let [message (<sub [::subs/message])]
      [:div
       {:class (str "global-status " type)
        :data-test (str "global-" type)}
       (when (= "success" type)
         [icon "tick" :class "tick is-hidden-desktop"])
       [icon "close" :class "close is-pulled-right" :on-click #(dispatch [:error/close-global])]
       [:div.global-status__body
        [:div.global-status__message
         {:data-test "global-status-message"}
         (when (= "success" type)
           [icon "tick" :class "tick is-hidden-mobile"])
         message
         (when-let [event (<sub [::subs/retry-event])]
           [:span " Please " [:span {:on-click #(do (.preventDefault %)
                                                    (dispatch [:error/retry-failed-action event]))
                                     :class    "global-status__try-again"}
                              "try again"]])]]])))

(defn loading-error []
  [:div.container
   [:h1 "Error"]
   [:div
    [:p "There was an error loading page \uD83D\uDE1E, please try again later."]]])
