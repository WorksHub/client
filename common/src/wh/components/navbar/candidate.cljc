(ns wh.components.navbar.candidate
  (:require [wh.components.icons :as icons]
            [wh.components.navbar.components :as components]
            [wh.components.navbar.shared :as navbar-shared]
            [wh.components.navbar.subs :as subs]
            [wh.re-frame.subs :refer [<sub]]
            [wh.routes :as routes]
            [wh.components.navbar.styles :as styles]
            [wh.util :as util]))

(def candidate-jobs-submenu-list
  [{:route      :jobsboard
    :icon-name  "board-rectangles"
    :icon-class styles/dropdown__link__icon-jobsboard
    :text       "Jobsboard"}
   {:route      :recommended
    :icon-name  "robot-face"
    :icon-class styles/dropdown__link__icon-robot
    :text       "Recommended Jobs"}
   {:route      :liked
    :icon-name  "save"
    :icon-class styles/dropdown__link__icon-save
    :text       "Saved Jobs"
    :data-test  "saved-jobs"}
   {:route      :applied
    :icon-name  "document"
    :icon-class styles/dropdown__link__icon-document
    :text       "Applied Jobs"}])

(def candidate-profile-submenu-list
  [{:route     :profile
    :icon-name "person"
    :text      "Profile settings"}
   {:route     :notifications-settings
    :icon-name "bell"
    :text      "Notification settings"}
   {:route             :logout
    :data-pushy-ignore "true"
    :icon-name         "logout"
    :text              "Logout"}])

(defn candidate-mobile-menu []
  [:div (util/smc styles/small-menu styles/small-menu--logged-in)
   [:div (util/smc styles/small-menu__column)
    [components/submenu
     {:text      "Jobs"
      :icon-name "suitcase"
      :dropdown  candidate-jobs-submenu-list}]
    [components/link-with-icon
     {:icon-name "git"
      :text      "Issues"
      :route     :issues}
     {:mobile? true}]
    [components/submenu
     {:icon-name "document"
      :text      "Articles"
      :route     :learn
      :dropdown  navbar-shared/articles-submenu-list}]
    [components/link-with-icon
     {:icon-name "union"
      :text      "Companies"
      :route     :companies}
     {:mobile? true}]]

   [:div (util/smc styles/small-menu__column)
    [components/link-with-icon
     {:icon-name "rocketship"
      :text      "Metrics"
      :route     :metrics}
     {:mobile? true}]

    (for [element candidate-profile-submenu-list]
      ^{:key (:text element)}
      [components/link-with-icon element
       {:mobile? true}])]])

(defn candidate-menu [page]
  [:ul (util/smc styles/links)
   [components/link {:children [icons/icon "home"
                                :class styles/home-icon]
                     ;; :path takes precedence over :route in setting :href value,
                     ;; but :route is used to decide whether link is active.
                     ;; All is well here
                     :path     (routes/path :homepage)
                     :route    :homepage-dashboard
                     :page     page}]

   [components/link
    {:text     "Jobs"
     :route    :jobsboard
     :page     page
     :dropdown candidate-jobs-submenu-list}]
   [components/link
    {:text     "Articles"
     :route    :learn
     :page     page
     :dropdown navbar-shared/articles-submenu-list}]
   [components/link
    {:text  "Companies"
     :route :companies
     :page  page}]])

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

    [components/dropdown-list candidate-profile-submenu-list]]])
