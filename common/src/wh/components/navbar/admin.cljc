(ns wh.components.navbar.admin
  (:require #?(:cljs [reagent.core :as r])
            [wh.components.icons :as icons]
            [wh.components.navbar.components :as components]
            [wh.components.navbar.shared :as navbar-shared]
            [wh.components.navbar.subs :as subs]
            [wh.re-frame.subs :refer [<sub]]
            [wh.routes :as routes]
            [wh.components.navbar.styles :as styles]
            [wh.util :as util]))

(def admin-profile-submenu-list
  [{:route     :profile
    :icon-name "person"
    :text      "Profile settings"}
   {:route     :notifications-settings
    :icon-name "bell"
    :text      "Notification settings"}
   {:route     :metrics
    :icon-name "rocketship"
    :text      "Metrics"}
   {:route     :feed
    :icon-name "feed"
    :text      "Feed"}
   {:route             :logout
    :data-pushy-ignore "true"
    :icon-name         "logout"
    :text              "Logout"}])

(defn jobs-admin-submenu-list []
  (remove
    nil?
    [{:path       (routes/path :jobsboard)
      :icon-name  "board-rectangles"
      :icon-class styles/dropdown__link__icon-jobsboard
      :text       "Jobsboard"}
     {:path       (routes/path :homepage)
      :icon-name  "document-filled"
      :icon-class styles/dropdown__link__icon-document
      :text       "All live Applications"}
     {:path       (routes/path :create-job-new)
      :icon-name  "plus-circle"
      :icon-class styles/dropdown__link__icon-plus
      :text       "Post a new Job"}
     {:path       (routes/path :recommended)
      :icon-name  "recommend"
      :icon-class styles/dropdown__link__icon-plus
      :text       "Recommended Jobs"}
     {:path       (routes/path :liked)
      :icon-name  "bookmark"
      :icon-class styles/dropdown__link__icon-plus
      :text       "Saved Jobs"}
     {:path       (routes/path :applied)
      :icon-name  "applications"
      :icon-class styles/dropdown__link__icon-plus
      :text       "Applied Jobs"}]))

(def company-admin-submenu-list
  [{:path       (routes/path :admin-companies)
    :icon-name  "recommend"
    :text       "All companies"}
   {:path       (routes/path :companies)
    :icon-name  "union"
    :icon-class styles/dropdown__link__icon-union
    :text       "Public companies"}
   {:path       (routes/path :create-company)
    :icon-name  "plus-circle"
    :icon-class styles/dropdown__link__icon-union
    :text       "Create company"}])

(def candidates-admin-submenu-list
  [{:path       (routes/path :candidates)
    :icon-name  "profile"
    :text       "All Candidates"}
   {:path       (routes/path :create-candidate)
    :icon-name  "plus-circle"
    :icon-class styles/dropdown__link__icon-plus
    :text       "Create candidate"}])

(defn admin-mobile-menu []
  [:div (util/smc styles/small-menu styles/small-menu--logged-in)
   [:div (util/smc styles/small-menu__column)
    [components/submenu
     {:text      "Jobs"
      :icon-name "suitcase"
      :dropdown  (jobs-admin-submenu-list)}]

    [components/submenu
     {:text      "Articles"
      :icon-name "document"
      :dropdown  navbar-shared/articles-admin-submenu-list}]

    [components/submenu
     {:text      "Companies"
      :icon-name "union"
      :dropdown  company-admin-submenu-list}]

    [components/submenu
     {:text      "Candidates"
      :icon-name "profile"
      :dropdown  candidates-admin-submenu-list}]]

   [:div (util/smc styles/small-menu__column)
    [components/link-with-icon
     {:icon-name "git"
      :text      "Issues"
      :route     :issues}]

    [components/dropdown-list admin-profile-submenu-list
     {:mobile? true}]]])

(defn admin-menu [{:keys [page]}]
  [:ul (util/smc styles/links)
   [components/link {:children [icons/icon "dashboard"
                                :class styles/dashboard-icon]
                     ;; :path takes precedence over :route in setting :href value,
                     ;; but :route is used to decide whether link is active.
                     ;; All is well here
                     :path     (routes/path :homepage)
                     :route    :admin-applications
                     :page     page}]

   [components/link
    {:text     "Jobs"
     :route    :jobsboard
     :page     page
     :dropdown (jobs-admin-submenu-list)}]

   [components/link {:text     "Companies"
                     :route    :admin-companies
                     :page     page
                     :dropdown company-admin-submenu-list}]

   [components/link
    {:text     "Articles"
     :route    :learn
     :page     page
     :dropdown navbar-shared/articles-admin-submenu-list}]

   [components/link
    {:text     "Candidates"
     :route    :candidates
     :page     page
     :dropdown candidates-admin-submenu-list}]])

(defn profile-menu []
  [:<>
   [navbar-shared/conversations-link {}]
   [:div {:class styles/user-profile-container}
    [:a {:href  (routes/path :profile)
         :class styles/user-profile}

     (if-let [profile-image (<sub [::subs/profile-image])]
       [:img {:class styles/profile-image
              :src   profile-image}]

       [:div {:class styles/profile-image}])
     [components/arrow-down]]

    [components/dropdown-list admin-profile-submenu-list]]])
