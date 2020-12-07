(ns wh.components.navbar.company
  (:require #?(:cljs [reagent.core :as r])
            [wh.components.icons :as icons]
            [wh.components.navbar.components :as components]
            [wh.components.navbar.subs :as subs]
            [wh.components.navbar.tasks :as navbar-tasks]
            [wh.re-frame.subs :refer [<sub]]
            [wh.routes :as routes]
            [wh.styles.navbar :as styles]
            [wh.util :as util]))

(defn company-profile-submenu-list [company-slug]
  [{:path      (routes/path :company :params {:slug company-slug})
    :icon-name "company-building"
    :text      "Company profile"}
   {:route     :edit-company
    :icon-name "settings"
    :text      "Company settings"}
   {:route     :profile
    :icon-name "person"
    :text      "My profile"}
   {:route     :feed
    :icon-name "feed"
    :text      "Feed"}
   {:route     :metrics
    :icon-name "rocketship"
    :text      "Metrics"}
   {:route             :logout
    :data-pushy-ignore "true"
    :icon-name         "logout"
    :text              "Logout"}])

(def issues-company-submenu-list
  [{:path       (routes/path :issues)
    :icon-name  "git"
    :icon-class styles/dropdown__link__icon-issues
    :text       "All Open Source Issues"}
   {:path       (routes/path :manage-issues)
    :icon-name  "plus-circle"
    :icon-class styles/dropdown__link__icon-plus
    :text       "Post an Issue"}
   {:path       (routes/path :company-issues)
    :icon-name  "person"
    :icon-class styles/dropdown__link__icon-person
    :text       "My Open Source Issues"}])

(defn jobs-company-submenu-list [can-publish?]
  [{:path       (routes/path :jobsboard)
    :icon-name  "board-rectangles"
    :icon-class styles/dropdown__link__icon-jobsboard
    :text       "Jobsboard"}
   {:path       (if can-publish?
                  (routes/path :create-job)
                  (routes/path :payment-setup
                               :params {:step :select-package}
                               :query-params {:action "publish"}))
    :icon-name  "plus-circle"
    :icon-class styles/dropdown__link__icon-plus
    :text       "Post a new Job"}
   {:path       (routes/path :company-applications)
    :icon-name  "document-filled"
    :icon-class styles/dropdown__link__icon-document
    :text       "All live Applications"}])

(defn company-company-submenu-list [company-slug]
  [{:path       (routes/path :companies)
    :icon-name  "union"
    :icon-class styles/dropdown__link__icon-union
    :text       "All companies"}
   {:path       (routes/path :company :params {:slug company-slug})
    :icon-name  "company-building"
    :icon-class styles/dropdown__link__icon-company
    :text       "Company profile"}])

(def articles-company-submenu-list
  [{:path       (routes/path :learn)
    :icon-name  "document"
    :icon-class styles/dropdown__link__icon-document
    :text       "All articles"}
   {:path       (routes/path :contribute)
    :icon-name  "plus-circle"
    :icon-class styles/dropdown__link__icon-plus
    :text       "Write an article"}])

(defn company-mobile-menu []
  (let [company-slug (<sub [::subs/company-slug])
        can-publish? (<sub [::subs/can-publish-jobs?])]
    [:div (util/smc styles/small-menu styles/small-menu--logged-in)
     [:div (util/smc styles/small-menu__column)
      [components/submenu
       {:text      "Jobs"
        :icon-name "suitcase"
        :dropdown  (jobs-company-submenu-list can-publish?)}]

      [components/submenu
       {:text      "Issues"
        :icon-name "git"
        :dropdown  issues-company-submenu-list}]

      [components/submenu
       {:text      "Articles"
        :icon-name "document"
        :dropdown  articles-company-submenu-list}]]

     [:div (util/smc styles/small-menu__column)
      [components/dropdown-list
       (company-profile-submenu-list company-slug)
       {:mobile? true}]]]))

(defn notifications [opts]
  #?(:cljs [navbar-tasks/tasks-notifications opts]))

(defn company-menu [{:keys [page]}]
  (let [can-publish? (<sub [::subs/can-publish-jobs?])]
    [:ul (util/smc styles/links)
     [components/link {:children [icons/icon "dashboard"
                                  :class styles/dashboard-icon]
                       ;; :path takes precedence over :route in setting :href value,
                       ;; but :route is used to decide whether link is active.
                       ;; All is well here
                       :path     (routes/path :homepage)
                       :route    :company-dashboard
                       :page     page}]

     [components/link
      {:text     "Jobs"
       :route    :jobsboard
       :page     page
       :dropdown (jobs-company-submenu-list can-publish?)}]

     [components/link
      {:text     "Open Source Issues"
       :route    :issues
       :page     page
       :dropdown issues-company-submenu-list}]

     (let [company-slug (<sub [::subs/company-slug])]
       [components/link {:text     "Company"
                         ;; :path takes precedence over :route in setting :href value,
                         ;; but :route is used to decide whether link is active.
                         ;; All is well here
                         :path     (routes/path :company :params {:slug company-slug})
                         :route    :company
                         :page     page
                         :dropdown (company-company-submenu-list company-slug)}])

     [components/link
      {:text     "Articles"
       :route    :learn
       :page     page
       :dropdown articles-company-submenu-list}]

     [notifications {}]]))

(defn profile-menu []
  (let [company-slug (<sub [::subs/company-slug])]
    [:div {:class styles/user-profile-container}
     [:a {:href  (routes/path :company :params {:slug company-slug})
          :class styles/user-profile}

      (if-let [profile-image (<sub [::subs/profile-image])]
        [:img {:class styles/profile-image
               :src   profile-image}]

        [:div {:class styles/profile-image}])
      [components/arrow-down]]

     [components/dropdown-list (company-profile-submenu-list company-slug)]]))
