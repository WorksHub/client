(ns wh.components.auth-popup
  (:require [wh.components.button-auth :as button-auth]
            [wh.components.icons :refer [icon]]
            [wh.interop :as interop]
            [wh.util :as util]))

(def auth-popup-id "auth-popup")

(defn build-sub-id
  [s]
  (str auth-popup-id "__" s))

(defn build-additional-content-id [s]
  (str "additional-content__" s))

(defn overlay-content-publish
  []
  [:div.auth-popup__content
   {:id (build-sub-id "publish")}
   [:h2 "Login or register" [:br] "to publish this job!"]])

(defn overlay-content-save
  []
  [:div.auth-popup__content
   {:id (build-sub-id "save")}
   [:h2 "Login or register" [:br] "to save this job!"]])

(defn overlay-content-save-blog
  []
  [:div.auth-popup__content
   {:id (build-sub-id "save-blog")}
   [:h2 "Login or register" [:br] "to save articles!"]])

(defn overlay-content-saved-jobs
  []
  [:div.auth-popup__content
   {:id (build-sub-id "saved-jobs")}
   [:h2 "Login or register" [:br] "to save interesting jobs!"]])

(defn overlay-content-recommended-jobs
  []
  [:div.auth-popup__content
   {:id (build-sub-id "recommended-jobs")}
   [:h2 "Login or register" [:br] "to get personalised job recommendations!"]])

(defn overlay-content-applied-jobs
  []
  [:div.auth-popup__content
   {:id (build-sub-id "applied-jobs")}
   [:h2 "Login or register" [:br] "to get access to all your job applications!"]])

(defn overlay-content-see-more
  []
  [:div.auth-popup__content
   {:id (build-sub-id "see-more")}
   [:h2 "Login or register" [:br] "to see more jobs from this company!"]])

(defn overlay-content-blog
  []
  [:div.auth-popup__content
   {:id (build-sub-id "upvote")}
   [:h2 "Login or register" [:br] "to boost this post!"]
   [:p "Show some love to the author of this blog by giving their post some rocket fuel 🚀."]])

(defn overlay-content-issue
  []
  [:div.auth-popup__content
   {:id (build-sub-id "issue")}
   [:h2 "Login or register to start working on this issue!"]])

(defn overlay-content-search-jobs
  []
  [:div.auth-popup__content
   {:id (build-sub-id "search-jobs")}
   [:h2 "Login or register to search for your ideal job!"]])

(defn overlay-content-contribute
  []
  [:div.auth-popup__content
   {:id (build-sub-id "contribute")}
   [:h2 "Login or register to start contributing with an article!"]])

(defn overlay-content-see-application
  []
  [:div.auth-popup__content
   {:id (build-sub-id "see-application")}
   [:h2 "Login to see the application"]])

;;

(defn ac-default [platform-name]
  [:div.auth-popup__additional-content
   {:id (build-additional-content-id "default")}
   [:p "Engineers who find a new job through " platform-name " average a 15% increase in salary \uD83D\uDE80 "]])

(defn ac-see-application []
  [:div.auth-popup__additional-content
   {:id (build-additional-content-id "see-application")}
   [:p "You will be redirected back to this page right after signin"]])

;;

(def button-params {:on-click (interop/hide-auth-popup)})
(def email-signup-params (-> {:text "Signup with Email"}
                             (merge button-params)))
(def email-signin-params (-> {:inverted? true}
                             (merge button-params)))

(defn popup [platform-name]
  [:div
   {:id    auth-popup-id
    :class (util/merge-classes "overlay" "auth-popup")}
   [:div.overlay__content
    [:div.overlay__close
     (interop/on-click-fn
       (interop/hide-auth-popup))
     [icon "close"]]
    [:div
     [overlay-content-publish]
     [overlay-content-save]
     [overlay-content-saved-jobs]
     [overlay-content-recommended-jobs]
     [overlay-content-applied-jobs]
     [overlay-content-contribute]
     [overlay-content-see-more]
     [overlay-content-blog]
     [overlay-content-search-jobs]
     [overlay-content-issue]
     [overlay-content-save-blog]
     [overlay-content-see-application]]
    [:div
     [ac-default platform-name]
     [ac-see-application]]
    [:div.overlay__buttons
     [button-auth/button :github button-params]
     [button-auth/button :twitter button-params]
     [button-auth/button :stackoverflow button-params]
     [button-auth/button :email-signup email-signup-params]
     [button-auth/button :email-signin email-signin-params]]]])
