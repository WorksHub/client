(ns wh.components.navbar
  (:require #?(:cljs [wh.pages.core :as pages])
            [clojure.string :as str]
            [wh.common.data :as data]
            [wh.common.data.company-profile :as company-data]
            [wh.common.url :as url]
            [wh.common.user :as user-common]
            [wh.components.common :refer [link]]
            [wh.components.icons :refer [icon]]
            [wh.components.navbar.navbar :as navbar]
            [wh.interop :as interop]
            [wh.re-frame :as r]
            [wh.re-frame.events :refer [dispatch]]
            [wh.re-frame.subs :refer [<sub]]
            [wh.routes :as routes]
            [wh.util :as util]
            [wh.verticals :as verticals]))

(def logged-out-menu-id             "logged-out-menu")
(def candidates-overlay-menu-id     "candidates-overlay-menu")
(def resources-overlay-menu-id      "resources-overlay-menu")
(def resources-overlay-roll-down-id "resources-overlay-menu-roll-down")
(def lth-overlay-menu-id            "lth-overlay-menu")
(def lth-overlay-roll-down-id       "lth-overlay-menu-roll-down")
;;
(def candidates-menu-id             "candidates-menu")
;;
(def mobile-search-id               "mobile-search")
(def promo-banner-id                "promo-banner")

;; the ids that represent
(def navbar-ids #{logged-out-menu-id
                  candidates-overlay-menu-id
                  resources-overlay-menu-id
                  resources-overlay-roll-down-id
                  lth-overlay-menu-id
                  lth-overlay-roll-down-id})

(def learn-link     (routes/path :learn))
(def pricing-link   (routes/path :pricing))
(def issues-link    (routes/path :issues))
(def jobsboard-link (routes/path :jobsboard))
(def companies-link (routes/path :companies))

(defn logo-title
  ([vertical]
   (case vertical
     "www" [:div.logo-title [:span.heavy-logo "WORKS"] "HUB"]
     "ai" [:div.logo-title [:span.heavy-logo (str/capitalize vertical)] "WORKS"]
     [:div.logo-title [:span.heavy-logo (str/upper-case vertical)] "WORKS"]))
  ([vertical env]
   [:a.logo-title-container
    {:href (url/vertical-homepage-href env vertical)}
    (logo-title vertical)]))

(defn candidates-menu-content
  [env id class link-class]
  [:div
   {:id id
    :class class}
   [:div "What's your area of expertise?"]
   (doall
     (for [vertical verticals/ordered-job-verticals]
       ^{:key vertical}
       [:a
        {:class link-class
         :href (url/vertical-homepage-href
                 (name env)
                 vertical)}
        (icon vertical)
        [:span (verticals/config vertical :platform-name)]]))])

(defn reset-base-opts
  [disable-no-scroll? & [toggle-is-open]]
  (apply interop/multiple-on-click
         (concat (conj (mapv (fn [id] (when-not (contains? toggle-is-open id)
                                        (interop/set-is-open-on-click id false))) navbar-ids)
                       (when disable-no-scroll? (interop/disable-no-scroll-on-click)))
                 (when (not-empty toggle-is-open)
                   (map interop/toggle-is-open-on-click toggle-is-open)))))

(defn resources-menu-content
  [env class]
  (let [base-opts (reset-base-opts false)]
    [:div
     {:class class}
     [:a (merge {:href issues-link} base-opts)
      [:h3 "Open Source Issues"]
      [:p "Improve your skillset and get paid to contribute to projects you find interesting"]]
     [:a (merge {:href companies-link} base-opts)
      [:h3 "Company Profiles"]
      [:p "Discover products and startups that you want to work for"]]
     [:a (merge {:href learn-link} base-opts)
      [:h3 "Articles"]
      [:p "Fresh thinking and unique perspectives from the most talented engineers"]]]))

(defn looking-to-hire-menu-content
  [env class]
  (let [base-opts (reset-base-opts false)]
    [:div
     {:class class}
     [:p "Here’s how we can source the best tech talent for your team…"]
     [:div.navbar-overlay--looking-to-hire-inner
      [:div
       [:a (merge {:href jobsboard-link} base-opts)
        [:h3 "Jobs Board"]
        [:p "Advertise your role on here and reach over 90,000 engineers"]]
       [:a (merge {:href companies-link} base-opts)
        [:h3 "Company Profiles"]
        [:p "Market your company to our community for free! Tell them what you're building and how"]]]
      [:div
       [:a (merge {:href issues-link} base-opts)
        [:h3 "Open Source Issues"]
        [:p "Engage with potential new hires through your open source code"]]
       [:a (merge {:href learn-link} base-opts)
        [:h3 "Articles"]
        [:p "Give insight into your company by sharing the stories that built your product"]]]]]))

(defn mobile-logged-out-menu
  [{:keys [vertical  env query-params]}]
  (let [link-opts (reset-base-opts true)
        primary-opts {:id logged-out-menu-id
                      :class "navbar-menu is-hidden-desktop"}]
    (if (= "www" vertical)
      [:div
       primary-opts
       [:div.navbar-menu-items
        [:div.navbar-menu-item.navbar-menu-item--candidates
         (interop/toggle-is-open-on-click candidates-menu-id)
         "Get Hired"]
        (candidates-menu-content env
                                 candidates-menu-id
                                 "navbar-menu--candidates"
                                 "navbar-menu--candidates__vertical-link")
        [:hr]
        [:a.navbar-menu-item (merge link-opts {:href jobsboard-link}) "Jobs"]
        [:hr]
        [:a.navbar-menu-item (merge link-opts {:href companies-link}) "Companies"]
        [:hr]
        [:a.navbar-menu-item (merge link-opts {:href issues-link}) "Issues"]
        [:hr]
        [:a.navbar-menu-item (merge link-opts {:href learn-link}) "Blog"]
        [:hr]
        [:a.navbar-menu-item (merge link-opts {:href pricing-link}) "Pricing"]
        [:hr]
        [:a.navbar-menu-item (merge {:href (routes/path :get-started)}) "Get Started"]]
       [:div.navbar-menu-close-area link-opts]]

      [:div
       primary-opts
       [:div.navbar-menu-items
        [:a.navbar-menu-item (merge link-opts {:href jobsboard-link}) "Jobs"]
        [:hr]
        [:a.navbar-menu-item (merge link-opts {:href companies-link}) "Companies"]
        [:hr]
        [:a.navbar-menu-item (merge link-opts {:href issues-link}) "Issues"]
        [:hr]
        [:a.navbar-menu-item (merge link-opts {:href learn-link}) "Learn"]
        [:hr]
        [:a.navbar-menu-item (merge link-opts {:href routes/company-landing-page}) "For Employers"]
        [:hr]
        [:a.navbar-menu-item (merge link-opts {:href (routes/path :get-started)}) "Get Started"]]
       [:div.navbar-menu-close-area link-opts]])))

(defn candidates-menu
  [{:keys [env query-params]}]
  [:div.navbar-overlay.navbar-overlay--candidates.is-hidden-mobile
   {:id candidates-overlay-menu-id}
   [:div.navbar-overlay__inner
    [:div.navbar-overlay__bg
     (interop/set-is-open-on-click candidates-overlay-menu-id false)]
    (candidates-menu-content env nil
                             "navbar-overlay__content"
                             "navbar-overlay--candidates__vertical-link")
    [:img {:src "/images/homepage/triangle.svg"
           :alt ""}]]])

(defn resources-menu
  [{:keys [env query-params]}]
  [:div.navbar-overlay.navbar-overlay--resources.is-hidden-mobile
   {:id resources-overlay-menu-id}
   [:div.navbar-overlay__inner
    [:div.navbar-overlay__bg
     (reset-base-opts false)]
    (resources-menu-content env
                            "navbar-overlay__content")
    [:img {:src "/images/homepage/triangle.svg"
           :alt ""}]]])

(defn looking-to-hire-menu
  [{:keys [env query-params]}]
  [:div.navbar-overlay.navbar-overlay--looking-to-hire.is-hidden-mobile
   {:id lth-overlay-menu-id}
   [:div.navbar-overlay__inner
    [:div.navbar-overlay__bg
     (reset-base-opts false)]
    (looking-to-hire-menu-content env
                                  "navbar-overlay__content")
    [:img {:src "/images/homepage/triangle.svg"
           :alt ""}]]])

#?(:cljs
   (defn submit-search
     [id]
     (pages/navigate [:jobsboard :query-params {:search (.-value (.getElementById js/document id))}])))

(defn search [default-value query-params tasks-open?]
  [:div.navbar__search
   [:form.navbar-item.is-hidden-mobile
    (merge {:method :get
            :action (routes/path :jobsboard)}
           #?(:cljs {:on-submit (fn [e]
                                  (.preventDefault e)
                                  (submit-search "navbar__search-input"))}))
    [:input.search (merge {:id "navbar__search-input"
                           :name "search"
                           :type "text"
                           :autoComplete "off"
                           :defaultValue default-value
                           :placeholder "Search for jobs..."}
                          #?(:cljs {:on-change #(reset! tasks-open? false)}))]
    (when default-value
      [:a.a--underlined.clear-search
       (merge {:id "navbar__clear-search-link"
               :href (routes/path :jobsboard)}
              #?(:cljs {:on-click #(set! (.-value (.getElementById js/document "navbar__search-input")))}))
       "Clear search"])
    [:button.search-button (merge {:id "navbar__search-btn"
                                   :aria-label "Search button"}
                                  #?(:cljs {:on-click #(reset! tasks-open? false)}))
     [icon "search-new"]]]
   [:div.navbar-item.is-hidden-desktop
    (interop/multiple-on-click (interop/set-is-open-on-click mobile-search-id true)
                               (interop/set-is-open-on-click data/logged-in-menu-id false)
                               (interop/set-no-scroll-on-click mobile-search-id true))
    [:button.search-button
     {:id "navbar__search-btn"
      :aria-label "Search button"} [icon "search-new"]]]])

(defn mobile-search
  [default-value query-params]
  [:div.mobile-search.is-hidden-desktop
   {:id mobile-search-id}
   [:div.mobile-search__close
    (interop/multiple-on-click (interop/set-is-open-on-click mobile-search-id false)
                               (interop/disable-no-scroll-on-click))
    [icon "close"]]
   [:form.mobile-search-form
    (merge {:method :get
            :action (routes/path :jobsboard)}
           #?(:clj  {:onSubmit (:onClick (interop/multiple-on-click (interop/set-is-open-on-click mobile-search-id false)
                                                                    (interop/disable-no-scroll-on-click)))}
              :cljs {:on-submit (fn [e]
                                  (.preventDefault e)
                                  (js/setClass mobile-search-id "is-open" false)
                                  (js/disableNoScroll)
                                  (submit-search "navbar__mobile-search-input"))}))
    [:button.search-button
     [icon "search-new"]]
    [:input.search
     {:id "navbar__mobile-search-input"
      :name "search"
      :type "text"
      :autoComplete "off"
      :defaultValue default-value
      :placeholder "Tap here to type..."}]]])

(defn task->path
  [task]
  (case task
    :complete_profile [:company :slug (some-> (<sub [:user/company]) :slug)]
    :add_job          [:create-job]
    :add_integration  [:edit-company]
    :add_issue        [:company-issues]))

(defn task
  [id {:keys [title subtitle] :as task} tasks-open?]
  (let [state (<sub [:user/company-onboarding-task-state id])
        [handler & {:as link-options}] (task->path id)]
    [:div
     {:key id
      :class (util/merge-classes "navbar__task"
                                 (when state (str "navbar__task--" (name state))))}
     [link {:text [:div
                   [:div.is-flex
                    [icon (:icon task)]
                    [:div.title title
                     [icon "cutout-tick"]]]
                   [:p subtitle]]
            :handler handler
            :options (assoc link-options
                            :on-click #(do (reset! tasks-open? false)
                                           #?(:cljs (js/setClass data/logged-in-menu-id "is-open" false))
                                           #?(:cljs (js/disableNoScroll))
                                           (dispatch [:company/set-task-as-read id])))}]]))

(defn unfinished-task-count
  []
  (->> company-data/company-onboarding-tasks
       (keys)
       (remove #(= :complete (<sub [:user/company-onboarding-task-state %])))
       (count)))

(defn task-notifications-content
  [tasks-open?]
  [:div.navbar-overlay__content
   [:div.navbar__tasks-header
    "Get more out of WorksHub"]
   (doall
     (for [[id t] company-data/company-onboarding-tasks]
       [:div.navbar__task-container
        {:key id}
        [task id t tasks-open?]]))])

(defn task-notifications
  [tasks-open?]
  (let [utc (unfinished-task-count)]
    [:div.navbar__tasks
     [icon "codi"
      :class #?(:cljs "navbar__tasks--clickable")
      :on-click #?(:cljs #(do
                            (when (swap! tasks-open? not)
                              (dispatch [:company/refresh-tasks]))))]
     #?(:cljs (when (pos? utc)
                [:small.navbar__unfinished-task-count utc]))
     (when @tasks-open?
       [:div.navbar__tasks__inner
        [:div.navbar-overlay__inner
         [:div.navbar-overlay__bg]
         [task-notifications-content tasks-open?]
         [:img {:src "/images/homepage/triangle2.svg"
                :alt ""}]]])]))

(defn navbar-end
  [{:keys [logged-in? query-params vertical show-navbar-menu? hide-search? tasks-open? show-tasks?]}]
  (let [el-id (if logged-in? data/logged-in-menu-id logged-out-menu-id)
        menu-roll-down [:div.navbar-item.is-hidden-desktop.navbar-item--menu
                        (interop/multiple-on-click (interop/toggle-is-open-on-click el-id)
                                                   (interop/toggle-no-scroll-on-click el-id)
                                                   (interop/set-is-open-on-click promo-banner-id false))
                        [:span "MENU"]
                        (when show-tasks?
                          (let [utc (unfinished-task-count)]
                            (when (pos? utc)
                              [:small.navbar__unfinished-task-count utc])))]
        base-opts (reset-base-opts true)]
    (if logged-in?
      [:div.navbar-search-end-wrapper
       (when-not hide-search?
         [:div.navbar-search-end.is-hidden-mobile
          (when show-tasks?
            [task-notifications tasks-open?])
          [search (:search query-params) query-params tasks-open?]])
       [:div.navbar-end.is-hidden-desktop
        (when-not hide-search? [search (:search query-params) query-params tasks-open?])
        (when show-navbar-menu? menu-roll-down)]]
      [:div.navbar-end
       [:a.navbar-item.navbar-item--login
        (merge
          {:href (routes/path :login :params {:step (if (= "www" vertical) :email :root)})}
          base-opts)
        "Login"]
       [:a.navbar-item.is-hidden-mobile.navbar-item--register
        (merge
          {:href (routes/path :get-started)}
          base-opts)
        [:button.button
         "Get Started"]]
       (when show-navbar-menu? menu-roll-down)])))

(defn navbar-content
  [{:keys [logged-in? query-params vertical]}]
  (cond
    logged-in? nil
    (= "www" vertical)
    [:div.navbar-items
     [:div.navbar-item.is-hidden-mobile.navbar-item--dropdown.navbar-item--looking-to-hire
      (reset-base-opts false #{lth-overlay-menu-id lth-overlay-roll-down-id})
      "Looking to hire?"
      [icon "roll-down" :id lth-overlay-roll-down-id]]
     [:a.navbar-item.is-hidden-mobile.navbar-item--pricing
      (merge {:href pricing-link}
             (reset-base-opts false)) "Pricing"]
     [:div.navbar-item.is-hidden-mobile.navbar-item--dropdown.navbar-item--candidates
      (reset-base-opts false #{candidates-overlay-menu-id})
      "Get Hired"]]
    :else
    (let [base-opts (reset-base-opts false)]
      [:div.navbar-items
       [:a.navbar-item.is-hidden-mobile.navbar-item--jobs
        (merge {:href jobsboard-link}
               base-opts) "Jobs"]
       [:div.navbar-item.is-hidden-mobile.navbar-item--dropdown.navbar-item--resources
        (reset-base-opts false #{resources-overlay-menu-id resources-overlay-roll-down-id})
        "Resources"
        [icon "roll-down" :id resources-overlay-roll-down-id]]
       [:a.navbar-item.is-hidden-mobile.navbar-item--hiring
        (merge {:href routes/company-landing-page}
               base-opts)
        "For Employers"]])))

(defn promo-banner [{:keys [page logged-in?]}]
  (when (and (contains? #{:pricing :how-it-works :issues :homepage :homepage-not-logged-in} page)
             (not logged-in?))
    [:div.navbar__promo-banner.is-open
     {:id "promo-banner"}
     [link "Hiring? Sign up and post your first job for FREE!" :register-company]]))

(defn top-bar
  [_args]
  (let [tasks-open? (r/atom false)]
    (fn [{:keys [env vertical logged-in? query-params page user-type] :as args}]
      (let [candidate? (user-common/candidate-type? user-type)
            args       (assoc args
                              :tasks-open? tasks-open?
                              :show-tasks? (= user-type "company"))
            content?   (not (contains? routes/no-nav-link-pages page))]
        [:nav {:class      (util/merge-classes "navbar")
               :id         "wh-navbar"
               :role       "navigation"
               :aria-label "main navigation"}
         (if (and logged-in? (not candidate?))
           [:div.navbar__content
              [:div.navbar-item.navbar__logo-container
               [:svg.icon.navbar__logo [icon vertical]]
               (logo-title vertical env)]
              (when content?
                [navbar-content args])
              (when content?
                [navbar-end args])
              (when (and content? (not logged-in?))
                [mobile-logged-out-menu args])
              (when content?
                [candidates-menu args])
              (when content?
                [resources-menu args])
              (when content?
                [looking-to-hire-menu args])
              (when content?
                [mobile-search (:search query-params) query-params])
              (when @tasks-open?
                [:div.navbar__fullscreen-intercept.is-hidden-mobile
                 {:on-click #(reset! tasks-open? false)}])]

           [navbar/navbar {:vertical     vertical
                           :page         page
                           :content?     content?
                           :env          env
                           :candidate?   candidate?
                           :query-params query-params}])]))))
