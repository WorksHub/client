(ns wh.register.verify
  (:require [wh.components.conversation.views :refer [codi-message button]]
            [wh.components.icons :refer [icon]]
            [wh.register.conversation :refer [next-button]]
            [wh.register.subs :as subs]
            [wh.subs :refer [<sub]]))

(defn panel []
  [:div.verify
   [codi-message "Perfect - thanks"]
   [codi-message "We need to complete one last check before creating your personal dashboard"]
   [codi-message "It’s a simple test, you just need to answer a coding question"]
   (when (and (not (<sub [::subs/connected-to-github?]))
              (not (<sub [::subs/stackoverflow-signup?]))
              (not (<sub [::subs/twitter-signup?])))
     [codi-message "Or you can skip this part by connecting to your Github account"]
     [button [[icon "github"] "Connect GitHub"] [:github/call] :class "button--github"])
   [button "Test me" [:register/advance]]])
