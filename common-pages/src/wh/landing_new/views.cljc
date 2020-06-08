;; Candidate landing page

(ns wh.landing-new.views
  (:require #?(:cljs [wh.pages.core :refer [load-and-dispatch]])
            [wh.common.data :as data]
            [wh.components.button-auth :as button-auth]
            [wh.components.cards :refer [blog-card]]
            [wh.components.carousel :refer [carousel]]
            [wh.components.job :refer [job-card]]
            [wh.components.common :refer [companies-section link wrap-img img]]
            [wh.components.icons :refer [icon]]
            [wh.components.www-homepage :as www :refer [animated-hr]]
            [wh.how-it-works.views :as hiw]
            [wh.landing-new.subs :as subs]
            [wh.re-frame.events :refer [dispatch]]
            [wh.re-frame.subs :refer [<sub]]))

(defn page []
  (let [top-blogs (<sub [::subs/top-blogs])]
    [:div
     [:h1 "Side columns"]
     [:section
      [:h2 "Top blogs"]
      [:ul
       (for [blog top-blogs]
         [:li (:title blog)])]]]))
