(ns wh.components.pods.candidates
  (:require
    [wh.components.common :refer [link]]
    [wh.re-frame.subs :refer [<sub]]
    [wh.util :as util]))

(defn candidate-cta
  [& [cls]]
  (when-not (<sub [:user/logged-in?])
    [:section
     {:class (util/merge-classes "pod"
                                 "pod--no-shadow"
                                 "candidate-cta-pod"
                                 cls)}
     [:h3 "Get hired! Sign up now and apply for roles at companies that interest you."]
     [:div.candidate-cta-pod__img
      [:img {:src "/images/get_started/candidate.svg"
             :alt ""}]]
     [:p (str "Engineers who find a new job through " (<sub [:wh/platform-name]) " average a 15% increase in salary.")]
     [link [:button.button.is-full-width "Get Started"] :register :step :email]]))
