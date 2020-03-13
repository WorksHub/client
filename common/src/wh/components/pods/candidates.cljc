(ns wh.components.pods.candidates
  (:require
    [wh.components.common :refer [link]]
    [wh.re-frame.subs :refer [<sub]]
    [wh.routes :as routes]
    [wh.util :as util]))

(defn oauth-github-button []
  [:a.button.button--public.button--github.button--github-integration.cta-pod__button
   {:href (routes/path :login :params {:step :github})}
   [:span "Get Started with"] [:div]])

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
     [oauth-github-button]
     [link [:button.button.is-full-width.cta-pod__button "Get Started"] :register :step :email]]))
