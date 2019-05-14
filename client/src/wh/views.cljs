(ns wh.views
  (:require
    [cljsjs.smoothscroll-polyfill]
    [clojure.walk :as walk]
    [re-frame.core :refer [dispatch dispatch-sync]]
    [reagent.core :as r]
    [wh.components.auth-popup.views :refer [auth-popup]]
    [wh.components.common :refer [link]]
    [wh.components.error.subs :as error-subs]
    [wh.components.error.views :refer [global-status-box]]
    [wh.components.footer :as footer]
    [wh.components.icons :refer [icon]]
    [wh.components.loader.views :refer [loader]]
    [wh.components.menu :as menu]
    [wh.components.navbar :as navbar]
    [wh.events :as events]
    [wh.pages.router :refer [current-page]]
    [wh.subs :as subs :refer [<sub]]))

(defn tracking-popup []
  (let [expanded (r/atom false)]
    (fn []
      [:div
       [:div.tracking-popup.is-hidden-mobile
        [:p "We use cookies and other tracking technologies to improve your browsing experience on our site, analyze site traffic,
    and understand where our audience is coming from. To find out more, please read our "
         [:a.a--underlined {:href   "/privacy-policy"
                            :target "_blank"
                            :rel    "noopener"}
          "privacy policy."]]
        [:p "By choosing 'I Accept', you consent to our use of cookies and other tracking technologies."]
        [:button.button {:on-click #(dispatch [::events/agree-to-tracking])} "I Accept"]]
       [:div.tracking-popup.is-hidden-desktop
        (if @expanded
          [:div
           [:p "We use cookies and other tracking technologies to improve your browsing experience on our site, analyze site traffic,
    and understand where our audience is coming from. To find out more, please read our "
            [:a.a--underlined {:href   "/privacy-policy"
                               :target "_blank"
                               :rel    "noopener"}
             "privacy policy."]]
           [:p
            "By choosing 'I Accept', you consent to our use of cookies and other tracking technologies. "
            [:a.a--underlined {:on-click #(reset! expanded false)} "Less"]]
           [:button.button {:on-click #(dispatch [::events/agree-to-tracking])} "I Accept"]]
          [:div.tracking-popup--collapsed
           [:p "We use cookies and other tracking technologies... " [:a.a--underlined {:on-click #(reset! expanded true)} "More"]]
           [:button.button {:on-click #(dispatch [::events/agree-to-tracking])} "I Accept"]])]])))

(defn version-mismatch []
  [:div.version-mismatch
   [icon "codi"]
   [:h1 "Sorry to interrupt you..."]
   [:p "...but we've just released a new version of our platform and we need you to reload the page so you can use it."]
   [:button.button.button--medium {:on-click #(js/window.location.reload true)} "Reload"]])

;; when other modules are loaded, extra components are conj'd onto this atom
;; (currently used by user, logged-in and blogs modules)
(def extra-overlays (r/atom []))

(defn overlays []
  (into
   [:div.overlays
    (when (<sub [::subs/display-tracking-consent-popup?])
      [tracking-popup])
    (when-not (<sub [:user/logged-in?])
      [auth-popup])]
   @extra-overlays))

(defn main-panel []
  (let [page (<sub [:wh.pages.core/page])
        user-type (<sub [:user/type])
        ;; specify links on the menu that should be restricted
        restricted-links (when-not (<sub [:company/has-permission? :can_see_applications])
                           #{:company-applications})
        query-params (walk/keywordize-keys (<sub [::subs/query-params]))]

    (if (<sub [::subs/version-mismatch])
      [version-mismatch]
      [:div.main-panel
       [navbar/top-bar
        {:env               (<sub [::subs/env])
         :vertical          (<sub [::subs/vertical])
         :logged-in?        (<sub [:user/logged-in?])
         :show-navbar-menu? (<sub [::subs/show-navbar-menu?])
         :query-params      query-params
         :page              page
         :user-type         user-type
         :restricted-links  restricted-links}]
       (when (<sub [::subs/show-left-menu?])
         [menu/menu
          user-type
          page
          restricted-links
          (:menu query-params)])
       [:div.page-container
        {:class (when-not (<sub [::subs/show-left-menu?]) "page-container--no-menu")}
        (when (<sub [::error-subs/message])
          [global-status-box])
        (if (<sub [::subs/loading?])
          [:div.main-wrapper
           [:div.loader-wrapper
            [loader]]]
          [current-page])
        (when (<sub [::subs/show-footer?])
          [footer/footer (<sub [::subs/vertical])])]
       [overlays]])))

(defonce remove-all-bsl-locks-when-app-loads
  (do
    (js/disableNoScroll)))
