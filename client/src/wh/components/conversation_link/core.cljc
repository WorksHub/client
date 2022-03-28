(ns wh.components.conversation-link.core
  (:require [wh.routes :as routes]
            [wh.components.conversation-link.styles :as styles]
            [wh.components.icons :refer [icon]]))

(defn link
  [{:keys [conversations-enabled?] :as _ctx}
   {:keys [id] :as _conversation}]
  (when (and conversations-enabled? id)
    [:a {:href  (routes/path :conversation :params {:id id})
         :class styles/link}
     [icon "message-circle"]
     "Open conversation"]))
