(ns wh.components.error.views
  (:require
    [re-frame.core :refer [dispatch]]
    [wh.components.error.subs :as subs]
    [wh.components.icons :refer [icon]]
    [wh.subs :refer [<sub]]))

(defn global-status-box []
  (when-let [message (<sub [::subs/message])]
    [:div
     {:class (str "global-status " (<sub [::subs/type]))}
     (when (= "success" (<sub [::subs/type]))
       [icon "tick" :class "tick is-hidden-desktop"])
     [icon "close" :class "close is-pulled-right" :on-click #(dispatch [:error/close-global])]
     [:div.global-status__body
      [:div.global-status__message
       (when (= "success" (<sub [::subs/type]))
         [icon "tick" :class "tick is-hidden-mobile"])
       message
       (when-let [event (<sub [::subs/retry-event])]
         [:span " Please " [:span {:on-click #(do (.preventDefault %)
                                                  (dispatch [:error/retry-failed-action event]))
                                   :class    "global-status__try-again"}
                            "try again"]])]]]))

;; TODO: eradicate this and use global-status-box everywhere instead
(defn error-box []
  (when-let [message (<sub [:wh.subs/errors])]
    [:article.message.is-danger.wh-error
     [:div.message-header
      [:p "An error occurred"]
      [:button.delete {:on-click #(do (.preventDefault %)
                                      (dispatch [:wh.pages/clear-errors]))}]]
     [:div.message-body (if (string? message)
                          message
                          [:ul (for [msg message]
                                 [:li {:key msg} msg])])]]))

(defn loading-error []
  [:div.container
   [:h1 "Error"]
   [:div
    [:p "There was an error loading page \uD83D\uDE1E, please try again later."]]])
