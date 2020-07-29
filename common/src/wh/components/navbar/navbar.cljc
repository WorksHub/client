(ns wh.components.navbar.navbar
  (:require [wh.common.url :as url]
            [wh.components.attract-card :as attract-card]
            [wh.components.branding :as branding]
            [wh.components.icons :as icons :refer [icon]]
            [wh.components.navbar.candidate :as candidate]
            [wh.components.navbar.company :as company]
            [wh.components.navbar.components :as components]
            [wh.components.navbar.subs :as subs]
            [wh.interop :as interop]
            [wh.re-frame.subs :refer [<sub]]
            [wh.routes :as routes]
            [wh.styles.navbar :as styles]
            [wh.util :as util]))

(defn logo [vertical env]
  [:a {:href      (url/vertical-homepage-href env vertical)
       :data-test "home-link"
       :class     styles/logo__wrapper}
   [icon vertical :class styles/logo__icon]
   [branding/vertical-title vertical {:size :small}]])

(defn search [type]
  [:form {:method :get
          :action (routes/path :jobsboard)
          :class  (util/mc
                    styles/search__wrapper
                    [(= type :small-menu-no-mobile) styles/no-mobile]
                    [(= type :desktop-plus-only) styles/desktop-plus-only])}
   [icon "search-new" :class styles/search__icon]
   [:input {:class       (util/mc
                           styles/search
                           [(or
                              (= type :small-menu)
                              (= type :small-menu-no-mobile))
                            styles/search__small-menu])
            :data-test   "job-search"
            :type        "text"
            :name        "search"
            :placeholder "Search for jobs..."
            ;; Reagent complains about autocomplete keyword
            #?(:cljs :autoComplete
               :clj  :autocomplete) "off"}]])

(defn button-write-article []
  [:a {:class     (util/mc styles/button styles/button--contribute)
       :href      (routes/path :contribute)}
   "Write article"])

(defn button-signup []
  [:a {:class     (util/mc styles/button styles/button--signup)
       :href      (routes/path :get-started)
       :data-test "register"}
   "Sign Up"])

(defn button-signin []
  [:a {:class (util/mc styles/button styles/button--signin)
       :href  (routes/path :login :params {:step :root})}
   "Sign In"])

(defn mobile-toggle-navigation-button []
  [:button (merge (util/smc styles/toggle-navigation)
                  (interop/multiple-on-click
                    (interop/toggle-menu-display)
                    (interop/set-is-open-on-click "promo-banner" false)))
   [icon "menu" :id "toggle-button-menu-icon"]
   [icon "close"
    :id "toggle-button-close-icon"
    :class styles/toggle-navigation__close-icon]])

(defn close-navigation-button []
  [:button (merge (util/smc styles/close-navigation)
                  (interop/multiple-on-click
                    (interop/toggle-menu-display)
                    (interop/set-is-open-on-click "promo-banner" false)))
   [icon "close" :class styles/close-navigation__icon]])


(defn public-mobile-menu []
  [:ul (util/smc styles/small-menu)
   [components/link-with-icon
    {:icon-name "suitcase"
     :text      "Jobs"
     :route     :jobsboard}
    {:mobile? true}]
   [components/link-with-icon
    {:icon-name "git"
     :text      "Issues"
     :route     :issues}
    {:mobile? true}]
   [components/link-with-icon
    {:icon-name "document"
     :text      "Articles"
     :route     :learn}
    {:mobile? true}]
   [components/link-with-icon
    {:icon-name "union"
     :text      "Companies"
     :route     :companies}
    {:mobile? true}]
   [components/link-with-icon
    {:icon-name "rocketship"
     :text      "Metrics"
     :route     :metrics}
    {:mobile? true}]
   [components/link-with-icon
    {:icon-name "couple"
     :text      "For Employers"
     :route     :employers}
    {:mobile? true}]])

(defn menu-for-mobile-and-tablet [{:keys [candidate? company?]}]
  (let [user? (or candidate? company?)]
    [:div {:class (util/mc styles/small-menu__wrapper)
           :id    "navigation"}
     [close-navigation-button]

     (when-not user?
       [:div (util/smc styles/small-menu__signin)
        [:div (util/smc styles/small-menu__signin-title) "Continue with"]
        [attract-card/signin-buttons]])

     (cond candidate?
           [candidate/candidate-mobile-menu]

           company?
           [company/company-mobile-menu]

           :else [public-mobile-menu])

     [:div (util/smc styles/small-menu__search-wrapper)
      [search :small-menu]]]))


(defn profile-menu [{:keys [candidate? company?]}]
  (let [company-slug (<sub [::subs/company-slug])]
    [:div {:class styles/user-profile-container}
     [:a {:href  (routes/path :profile)
          :class styles/user-profile}
      (if-let [profile-image (<sub [::subs/profile-image])]
        [:img {:class styles/profile-image
               :src   profile-image}]

        [:div {:class styles/profile-image}])]

     (cond
       company?
       [components/dropdown-list
        (company/company-profile-submenu-list company-slug)]

       candidate?
       [components/dropdown-list
        candidate/candidate-profile-submenu-list])]))

(defn navbar-right [{:keys [content? candidate? company?]}]
  (cond
    (or candidate? company?)
    [:div (util/smc styles/navbar__right)
     (when company? [company/notifications {:class styles/no-desktop}])

     [search :small-menu-no-mobile]
     [:div (util/smc styles/navbar__buttons)
      (when candidate? [button-write-article])

      (cond
        company?
        [company/profile-menu]

        candidate?
        [candidate/profile-menu])

      [mobile-toggle-navigation-button]]]

    content?
    [:div (util/smc styles/navbar__right)
     [search :desktop-plus-only]
     [:div (util/smc styles/navbar__buttons)
      [button-signup]
      [button-signin]
      [mobile-toggle-navigation-button]]]

    :else nil))

(defn public-menu [page]
  [:ul (util/smc styles/links)
   [components/link
    {:text  "Jobs"
     :route :jobsboard
     :page  page}]
   [components/link
    {:text  "Open Source Issues"
     :route :issues
     :page  page}]
   [components/link
    {:text  "Articles"
     :route :learn
     :page  page}]
   [components/link
    {:text  "Companies"
     :route :companies
     :page  page}]
   [components/link
    {:text  "Metrics"
     :route :metrics
     :page  page}]])


(defn navbar [{:keys [vertical env content? page query-params candidate? company?]
               :as opts}]
  [:div {:class     styles/navbar__wrapper
         :data-test "navbar"}
   [:div (util/smc styles/navbar)
    [logo vertical env]

    (when content?
      (cond
        company?
        [company/company-menu opts]

        candidate?
        [candidate/candidate-menu page]

        :else [public-menu page]))

    [navbar-right opts]]

   [menu-for-mobile-and-tablet opts]])
