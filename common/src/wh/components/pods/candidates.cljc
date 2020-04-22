(ns wh.components.pods.candidates
  (:require
    [wh.components.button-auth :as button-auth]
    [wh.components.common :refer [link]]
    [wh.components.icons :refer [icon]]
    [wh.re-frame.subs :refer [<sub]]
    [wh.util :as util]))

(defn candidate-cta
  [& [cls]]
  (when-not (<sub [:user/logged-in?])
    [:section
     {:class (util/merge-classes "pod"
                                 "pod--no-shadow"
                                 "cta-pod"
                                 cls)}
     [:h3.cta-pod__title "Get hired!"]
     [:h4.cta-pod__subtitle "Sign up now and apply for roles at companies that interest you."]
     [:img.cta-pod__img {:src "/images/get_started/candidate.svg"
                         :alt ""}]
     [:p.cta-pod__description "Engineers who find a new job through " (<sub [:wh/platform-name])
                                         " average a " [:i "15%"]  " increase in salary."]
     [button-auth/button :github {:class "cta-pod__auth-button"}]
     [button-auth/button :stackoverflow {:class "cta-pod__auth-button"}]
     [button-auth/button :email-signup {:class "cta-pod__auth-button"}]]))
