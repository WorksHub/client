(ns wh.components.navbar.navbar
  (:require [wh.common.url :as url]
            [wh.components.attract-card :as attract-card]
            [wh.components.branding :as branding]
            [wh.components.icons :as icons :refer [icon]]
            [wh.components.navbar.subs :as subs]
            [wh.interop :as interop]
            [wh.re-frame.subs :refer [<sub]]
            [wh.routes :as routes]
            [wh.styles.navbar :as styles]
            [wh.util :as util]))


(defn dropdown-element [{:keys [path icon icon-class text sub-text]}]
  [:li (util/smc styles/dropdown__element)
   [:a {:class (util/mc styles/dropdown__link)
        :href  path}
    [icons/icon icon
     :class (util/mc styles/dropdown__link__icon icon-class)]
    [:span (util/smc styles/dropdown__link__text) text]
    (when sub-text
      [:span (util/smc styles/dropdown__link__sub-text) sub-text])]])

(defn dropdown-list* [dropdown]
  (for [el dropdown]
    [dropdown-element el]))

(defn dropdown-list [dropdown]
  [:ul (util/smc styles/dropdown)
   (dropdown-list* dropdown)])

(defn link [{:keys [text route page dropdown]}]
  [:li (util/smc styles/link__wrapper [(= page route) styles/link__wrapper-active])
   [:a {:href      (routes/path route)
        :class     styles/link
        :data-test (name route)}
    (if dropdown
      [:span (util/smc styles/link--with-dropdown)
       [:span text]
       [icon "arrow-down" :class styles/arrow-down]]

      text)]

   (when dropdown
     [dropdown-list dropdown])])


(defn link-with-icon [{:keys [text route icon-name dropdown]}]
  [:li (util/smc styles/link__wrapper)
   [:a {:href  (routes/path route)
        :class (util/mc styles/link styles/link--with-icon)}

    [icon icon-name :class styles/link__icon]
    [:span text]]])

(defn logo [vertical env]
  [:a {:href      (url/vertical-homepage-href env vertical)
       :data-test "home-link"
       :class     styles/logo__wrapper}
   [icon vertical :class styles/logo__icon]
   [branding/vertical-title vertical nil]])

(defn search [type]
  [:form {:method :get
          :action (routes/path :jobsboard)
          :class  (util/mc
                    styles/search__wrapper
                    [(= type :small-menu-no-mobile) styles/no-mobile]
                    [(= type :desktop-plus-only) styles/desktop-plus-only])}
   [icon "search-new" :class styles/search__icon]
   [:input {:class        (util/mc
                            styles/search
                            [(or
                               (= type :small-menu)
                               (= type :small-menu-no-mobile))
                             styles/search__small-menu])
            :data-test    "job-search"
            :type         "text"
            :name         "search"
            :autocomplete "off"
            :placeholder  "Search for jobs..."}]])

(defn user-image []
  [:a {:href  (routes/path :profile)
       :class styles/user-profile}
   (if-let [user-image (<sub [::subs/user-image])]
     [:img {:class styles/user-image
            :src   user-image}]

     [:div {:class styles/user-image}])])

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

(def jobs-submenu-list
  [{:path       (routes/path :jobsboard)
    :icon       "jobs-board"
    :icon-class styles/dropdown__link__icon-jobsboard
    :text       "Jobsboard"}
   {:path       (routes/path :recommended)
    :icon       "robot-face"
    :icon-class styles/dropdown__link__icon-robot
    :text       "Recommended Jobs"}
   {:path       (routes/path :liked)
    :icon       "save"
    :icon-class styles/dropdown__link__icon-save
    :text       "Saved Jobs"}
   {:path       (routes/path :applied)
    :icon       "document"
    :icon-class styles/dropdown__link__icon-document
    :text       "Applied Jobs"}])

(defn jobs-submenu [candidate? page]
  (if candidate?
    [link {:text     "Jobs"
           :route    :jobsboard
           :page     page
           :dropdown jobs-submenu-list}]

    [link {:text  "Jobs"
           :route :jobsboard
           :page  page}]))

(defn menu-for-mobile-and-tablet [candidate?]
  [:div {:class (util/mc styles/small-menu__wrapper)
         :id    "navigation"}
   [close-navigation-button]

   (when-not candidate?
     [:div (util/smc styles/small-menu__signin)
      [:div (util/smc styles/small-menu__signin-title) "Continue with"]
      [attract-card/signin-buttons]])

   [:ul (util/smc styles/small-menu
                  (when candidate? styles/small-menu--candidate))
    (concat
      (if candidate?
        ;; On mobile menu we don't display "dropdown", just elements
        ;; of dropdown as part of the list
        (dropdown-list* jobs-submenu-list)

        [[link-with-icon {:icon-name "suitcase"
                          :text      "Jobs"
                          :route     :jobsboard}]])

      [[link-with-icon {:icon-name "git"
                        :text      "Issues"
                        :route     :issues}]
       [link-with-icon {:icon-name "document"
                        :text      "Articles"
                        :route     :learn}]
       [link-with-icon {:icon-name "union"
                        :text      "Companies"
                        :route     :companies}]
       [link-with-icon {:icon-name "rocketship"
                        :text      "Metrics"
                        :route     :metrics}]

       (when candidate?
         [link-with-icon {:icon-name "feed"
                          :text      "Feed"
                          :route     :feed}])

       (when-not candidate?
         [link-with-icon {:icon-name "couple"
                          :text      "For Employers"
                          :route     :employers}])])]
   [:div (util/smc styles/small-menu__search-wrapper)
    [search :small-menu]]])

(defn navbar-right [content? candidate?]
  (cond
    (and content? (not candidate?))
    [:div (util/smc styles/navbar__right)
     [search :desktop-plus-only]
     [:div (util/smc styles/navbar__buttons)
      [button-signup]
      [button-signin]
      [mobile-toggle-navigation-button]]]

    candidate?
    [:div (util/smc styles/navbar__right)
     [search :small-menu-no-mobile]
     [:div (util/smc styles/navbar__buttons)
      [button-write-article]
      [user-image]
      [mobile-toggle-navigation-button]]]

    :else nil))

(defn navbar [{:keys [vertical env content? page query-params candidate?]}]
  [:div {:class     styles/navbar__wrapper
         :data-test "navbar"}
   [:div (util/smc styles/navbar)
    [logo vertical env]

    (when content?
      [:ul (util/smc styles/links)
       [jobs-submenu candidate? page]

       [link {:text  "Open Source Issues"
              :route :issues
              :page  page}]
       [link {:text  "Articles"
              :route :learn
              :page  page}]
       [link {:text  "Companies"
              :route :companies
              :page  page}]
       ;; Removing temporarily to make space for Job Search
       #_[link {:text  "Metrics"
                :route :metrics
                :page  page}]
       (when candidate?
         [link {:text  "Feed"
                :route :feed
                :page  page}])])

    [navbar-right content? candidate?]]

   [menu-for-mobile-and-tablet candidate?]])
