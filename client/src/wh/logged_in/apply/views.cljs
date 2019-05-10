(ns wh.logged-in.apply.views
  (:require [re-frame.core :refer [dispatch]]
            [wh.components.icons :refer [icon]]
            [wh.logged-in.apply.cv :as cv]
            [wh.logged-in.apply.events :as events]
            [wh.logged-in.apply.subs :as subs]
            [wh.logged-in.apply.thanks :as thanks]
            [wh.subs :refer [<sub]]
            [wh.views]))

(defn chatbot []
  [:div.chatbot
   {:class (when (<sub [::subs/loading?]) " chatbot--loading")}
   [icon "close"
    :class "close is-pulled-right"
    :id "application-bot_close-bot"
    :on-click #(dispatch [::events/close-chatbot])]
   (if (<sub [::subs/loading?])
     [:svg.loader-1 [:use {:xlinkHref "#loader-1"}]]
     (condp = (<sub [::subs/step])
       :cv [cv/panel]
       :thanks [thanks/panel]
       nil))])

(defn overlay-apply []
  (when (<sub [::subs/display-chatbot?])
    [chatbot]))

(swap! wh.views/extra-overlays conj [overlay-apply])
