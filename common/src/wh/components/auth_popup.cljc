(ns wh.components.auth-popup
  (:require [wh.components.common :refer [link]]
            [wh.components.icons :refer [icon]]
            [wh.interop :as interop]
            [wh.util :as util]))

(def auth-popup-id "auth-popup")

(defn build-sub-id
  [s]
  (str auth-popup-id "__" s))

(defn overlay-content-apply
  []
  [:div.auth-popup__content
   {:id (build-sub-id "apply")}
   [:h1 "Login or register" [:br] "to apply for this job!"]])

(defn overlay-content-see-more
  []
  [:div.auth-popup__content
   {:id (build-sub-id "see-more")}
   [:h1 "Login or register" [:br] "to see the full job description!"]])

(defn overlay-content-blog
  []
  [:div.auth-popup__content
   {:id (build-sub-id "upvote")}
   [:h1 "Login or register" [:br] "to boost this post!"]
   [:p "Show some love to the author of this blog by giving their post some rocket fuel 🚀."]])

(defn overlay-content-issue
  []
  [:div.auth-popup__content
   {:id (build-sub-id "issue")}
   [:h1 "Login or register to start working on this issue!"]])

(defn overlay-content-search-jobs
  []
  [:div.auth-popup__content
   {:id (build-sub-id "search-jobs")}
   [:h1 "Login or register to search for your ideal job!"]])

(defn popup [platform-name]
  [:div
   {:id auth-popup-id
    :class (util/merge-classes "overlay" "auth-popup")}
   [:div.overlay__content
    [:div.overlay__close
     (interop/on-click-fn
      (interop/hide-auth-popup))
     [icon "close"]]
    [:div
     [overlay-content-apply]
     [overlay-content-see-more]
     [overlay-content-blog]
     [overlay-content-search-jobs]
     [overlay-content-issue]]
    [:div
     [:p "Engineers who find a new job through " platform-name  " average a 15% increase in salary \uD83D\uDE80 "]]
    [:div.overlay__buttons

     ;; Create Account
     [link [:button.button.button--large
            {:id "auth-popup__create-account"}
            [icon "profile" :class "button__icon"] "Create Account"]
      :register :step :email
      :on-click (interop/do
                  #_(interop/analytics-track)
                  (interop/hide-auth-popup))]

     ;; Login with GitHub
     [link [:button.button.button--large.button--light
            {:id "auth-popup__github"}
            [icon "github" :class "button__icon button__icon--light"] "Login with GitHub"]
      :login :step :github
      :on-click (interop/do
                  #_(interop/analytics-track)
                  (interop/hide-auth-popup))]

     ;; Login with Email
     [link [:button.button.button--large.button--light
            {:id "auth-popup__magic-link"}
            [icon "magic-link" :class "button__icon button__icon--light"] "Login with Email"]
      :login :step :email
      :on-click (interop/do
                  #_(interop/analytics-track)
                  (interop/hide-auth-popup))]]]])
