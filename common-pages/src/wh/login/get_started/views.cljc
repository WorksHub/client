(ns wh.login.get-started.views
  (:require [wh.routes :as routes]))

(defn page
  []
  [:div.get-started
   [:h2 "Please tell us if you're a"]
   [:div.get-started__choices
    [:a.get-started__choice
     {:href (routes/path :register)
      :id "get-started__candidate"}
     [:img {:src "/images/get_started/candidate.svg"}]
     [:button.button.button--public "Candidate"]]
    [:a.get-started__choice
     {:href (routes/path :register-company)
      :id "get-started__company"}
     [:img {:src "/images/get_started/company.svg"}]
     [:button.button.button--public "Company"]]]])
