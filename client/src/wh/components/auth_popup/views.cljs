(ns wh.components.auth-popup.views
  (:require
    [re-frame.core :refer [dispatch]]
    [wh.components.auth-popup.events]
    [wh.components.auth-popup.subs :as subs]
    [wh.components.common :refer [link]]
    [wh.components.icons :refer [icon]]
    [wh.pages.core :refer [load-and-dispatch]]
    [wh.subs :refer [<sub]]))

(defn contribute-overlay-content []
  [:div
   [:h1 "Login or register" [:br] "to create a blog!"]])

(defn overlay-content-apply []
  [:div
   [:h1 "Login or register" [:br] "to apply for this job!"]])

(defn overlay-content-see-more []
  [:div
   [:h1 "Login or register" [:br] "to see the full job description!"]])

(defn overlay-content-blog []
  [:div
   [:h1 "Login or register" [:br] "to boost this post!"]
   [:p "Show some love to " (<sub [:wh.blogs.blog.subs/author]) " by giving their post some rocket fuel 🚀."]])

(defn overlay-content-issue []
  [:div
   [:h1 "Login or register to start working on this issue!"]])

(defn auth-popup []
  (when-let [context (<sub [::subs/context])]
    [:div.overlay {:class (when (<sub [::subs/visible?]) "overlay--visible")}
     [:div.overlay__content
      [icon "close" :class "overlay__close" :on-click #(dispatch [:auth/hide-popup])]
      (condp contains? (:type context)
        #{:homepage-jobcard-apply :jobcard-apply :jobpage-apply} [overlay-content-apply]
        #{:homepage-jobcard-more-info :jobpage-see-more} [overlay-content-see-more]
        #{:upvote} [overlay-content-blog]
        #{:issue} [overlay-content-issue]
        #{:homepage-contribute} [contribute-overlay-content])
      [:div
       [:p "Engineers who find a new job through " (<sub [:wh.subs/platform-name]) " average a 15% increase in salary \uD83D\uDE80 "]]
      [:div.overlay__buttons
       [link [:button.button.button--large
              {:id "auth-popup__create-account"}
              [icon "profile" :class "button__icon"] "Create Account"]
        :register :step :email
        :on-click #(do (dispatch [:register/track-start context])
                       (dispatch [:auth/hide-popup]))]
       [:button.button.button--large.button--light
        {:id       "auth-popup__github"
         :on-click #(do (dispatch [:auth/hide-popup])
                        (load-and-dispatch [:login [:github/call context]]))}
        [icon "github" :class "button__icon button__icon--light"]
        "Login with GitHub"]
       [:button.button.button--large.button--light
        {:id       "auth-popup__magic-link"
         :on-click #(do (dispatch [:auth/hide-popup])
                        (dispatch [:wh.events/nav :login :params {:step :email}]))}
        [icon "magic-link" :class "button__icon button__icon--light"]
        "Login with Email"]
       ]]]))
