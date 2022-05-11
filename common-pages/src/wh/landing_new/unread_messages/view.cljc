(ns wh.landing-new.unread-messages.view
  (:require [wh.components.activities.components :as activities]
            [wh.landing-new.unread-messages.styles :as styles]
            [wh.routes :as routes]))

(defn unread-messages []
  [:div {:class styles/card}
   [:div {:class styles/wrapper}
    [:div {:class styles/red-dot}]
    [:span "You have unread messages"]]
   [activities/button
    {:href (routes/path :conversations) :class styles/button}
    "Open conversations"]])
