;; Candidate landing page

(ns wh.landing.views
  (:require
    [wh.components.common :refer [companies-section link]]
    [wh.components.github :refer [integrate-github-button]]
    [wh.components.www-homepage :refer [animated-hr]]
    #?(:cljs [wh.pages.core :refer [load-and-dispatch]])
    [wh.re-frame.events :refer [dispatch]]))

(defn header []
  [:div.homepage__header
   [:div.homepage__header__img
    [:div.homepage__header__img-inner
     [:img {:src "/images/homepage/header.svg"}]]]
   [:div.homepage__header__copy
    [:h1 "Discover the best functional programming opportunities"]
    [:p "We match your skills to great jobs using languages you love."]
    [:div.landing__header__buttons
     [integrate-github-button
      {:label "Get Started with"
       :user-type :candidate}]
     (link [:button.button
            {:id "www-landing__hero"}
            "Get Started"] :get-started)]]])

(defn page []
  [:div.homepage.landing-page
   [:div.homepage__top-content
    [:div.homepage__top-content__container
     (header)]
    [:div.homepage__companies-section
     [:div.homepage__companies-section__container
      (companies-section "Join engineers from:")]]]
   [:div.homepage__middle-content
    [:div.homepage__middle-content__container
     [:h2 "BROWSE ROLES AT THE TOP TECH COMPANIES"]
     [:h3 "See who’s hiring"]
     [:div.homepage__feature-ctas
      (link [:button.button
             "View All Jobs"] :jobs-board)]]
    [animated-hr "/images/homepage/rocket.svg" "homepage__animated-hr__rocket"]
    [:div.homepage__middle-content__container
     [:h2 "LEARN, COLLABORATE AND IMPROVE YOUR SKILLSET"]
     [:h3 "Open source issues"]]
    [animated-hr nil nil]
    [:div.homepage__middle-content__container
     [:h2 "LEARN AND CONTRIBUTE"]
     [:h3 "Articles that matter to you"]]
    [animated-hr "/images/homepage/globe.svg" "homepage__animated-hr__globe"]
    [:div.homepage__middle-content__container
     [:h2 "AND FINALLY…"]
     [:h3 "How we’re different"]]]
   [:div.homepage__bottom-content
    [:div.homepage__bottom-content__container]]])
