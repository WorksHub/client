(ns wh.login.invalid-magic-link.views
  (:require [wh.components.common :refer [link]]))

(defn page []
  [:div.main
   [:h1 "Magic Link"]
   [:h2 "Your magic link expired or is not valid. Please "
    [link "try logging in" :login :step :root :class "a--underlined"] " again."]])
