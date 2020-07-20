(ns wh.components.navbar.navbar
  (:require
    [re-frame.core :refer [dispatch]]
    [wh.components.attract-card :as attract-card]
    [wh.components.branding :as branding]
    [wh.components.icons :refer [icon]]
    [wh.re-frame.subs :refer [<sub]]
    [wh.routes :as routes]
    [wh.common.url :as url]
    [wh.interop :as interop]
    [wh.styles.navbar :as styles]
    [wh.util :as util]))

(defn link [{:keys [text route page]}]
  [:li (util/smc styles/link__wrapper [(= page route) styles/link__wrapper-active])
   [:a {:href (routes/path route)
        :class styles/link
        :data-test (name route)}
    text]])

(defn link-with-icon [{:keys [text route icon-name]}]
  [:li (util/smc styles/link__wrapper)
   [:a {:href (routes/path route) :class (util/mc styles/link styles/link--with-icon)}
    [icon icon-name :class styles/link__icon]
    text]])

(defn logo [vertical env]
  [:a {:href (url/vertical-homepage-href env vertical)
       :class styles/logo__wrapper}
   [icon vertical :class styles/logo__icon]
   [branding/vertical-title vertical nil]])

(defn search [type]
  [:form {:method :get
          :action (routes/path :jobsboard)
          :class (util/mc styles/search__wrapper [(= type :desktop-plus-only) styles/desktop-plus-only])}
   [icon "search-new" :class styles/search__icon]
   [:input {:class (util/mc styles/search [(= type :small-menu) styles/search__small-menu])
            :type "text"
            :name "search"
            :autocomplete "off"
            :placeholder "Search for jobs..."}]])

(defn button-signup []
  [:a {:class (util/mc styles/button styles/button--signup)
       :href (routes/path :get-started)
       :data-test "register"}
   "Sign Up"])

(defn button-signin []
  [:a {:class (util/mc styles/button styles/button--signin)
       :href (routes/path :login :params {:step :root})}
   "Sign In"])

(defn mobile-toggle-navigation-button []
  [:button (merge (util/smc styles/toggle-navigation)
                  (interop/multiple-on-click (interop/toggle-menu-display)
                                             (interop/set-is-open-on-click "promo-banner" false)))
   [icon "menu" :id "toggle-button-menu-icon"]
   [icon "close" :id "toggle-button-close-icon" :class styles/toggle-navigation__close-icon]])

(defn close-navigation-button []
  [:button (merge (util/smc styles/close-navigation)
                  (interop/multiple-on-click (interop/toggle-menu-display)
                                             (interop/set-is-open-on-click "promo-banner" false)))
   [icon "close" :class styles/close-navigation__icon]])

(defn menu-for-mobile-and-tablet [query-params]
  [:div {:class styles/small-menu__wrapper
         :id "navigation"}
   [close-navigation-button]
   [:div (util/smc styles/small-menu__signin)
    [:div (util/smc styles/small-menu__signin-title) "Continue with"]
    [attract-card/signin-buttons]]
   [:ul (util/smc styles/small-menu)
    [link-with-icon {:icon-name "suitcase"
                     :text "Jobs"
                     :route :jobsboard}]
    [link-with-icon {:icon-name "git"
                     :text "Issues"
                     :route :issues}]
    [link-with-icon {:icon-name "document"
                     :text "Articles"
                     :route :learn}]
    [link-with-icon {:icon-name "union"
                     :text "Companies"
                     :route :companies}]
    [link-with-icon {:icon-name "rocketship"
                     :text "Metrics"
                     :route :metrics}]
    [link-with-icon {:icon-name "couple"
                     :text "For Employers"
                     :route :employers}]]
   [:div (util/smc styles/small-menu__search-wrapper)
    [search :small-menu]]])

(defn navbar [{:keys [vertical env content? page query-params]}]
  [:div {:class styles/navbar__wrapper
         :data-test "navbar"}
   [:div (util/smc styles/navbar)
    [logo vertical env]
    (when content? [:ul (util/smc styles/links)
                    [link {:text "Jobs"
                           :route :jobsboard
                           :page page}]
                    [link {:text "Open Source Issues"
                           :route :issues
                           :page page}]
                    [link {:text "Articles"
                           :route :learn
                           :page page}]
                    [link {:text "Companies"
                           :route :companies
                           :page page}]
                    [link {:text "Metrics"
                           :route :metrics
                           :page page}]])
    (when content? [:div (util/smc styles/navbar__right)
                    [search :desktop-plus-only]
                    [:div (util/smc styles/navbar__buttons)
                     [button-signup]
                     [button-signin]
                     [mobile-toggle-navigation-button]]])]
   [menu-for-mobile-and-tablet query-params]])

