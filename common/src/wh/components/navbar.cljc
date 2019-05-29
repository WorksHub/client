(ns wh.components.navbar
  (:require
    #?(:cljs [re-frame.core :refer [dispatch dispatch-sync]])
    #?(:cljs [wh.pages.core :as pages])
    [clojure.string :as str]
    [wh.common.url :as url]
    [wh.components.common :refer [link]]
    [wh.components.icons :refer [icon]]
    [wh.components.menu :as menu]
    [wh.interop :as interop]
    [wh.routes :as routes]
    [wh.verticals :as verticals]))

(def candidates-overlay-menu-id "candidates-overlay-menu")
(def candidates-menu-id         "candidates-menu")
(def logged-out-menu-id         "logged-out-menu")
(def mobile-search-id           "mobile-search")

(def learn-link     (routes/path :learn))
(def pricing-link   (routes/path :pricing))
(def issues-link    (routes/path :issues))
(def jobsboard-link (routes/path :jobsboard))

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

(defn logged-out-menu
  [{:keys [vertical  env query-params]}]
  (let [link-opts (interop/multiple-on-click (interop/set-is-open-on-click logged-out-menu-id false)
                                             (interop/disable-no-scroll-on-click))
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
        [:a.navbar-menu-item (merge link-opts {:href learn-link}) "Blog"]
        [:hr]
        [:a.navbar-menu-item (merge link-opts {:href pricing-link}) "Pricing"]
        [:hr]
        [:a.navbar-menu-item (merge link-opts {:href issues-link}) "Open Source Issues"]
        [:hr]
        [:a.navbar-menu-item (merge {:href (routes/path :get-started)}) "Get Started"]]
       [:div.navbar-menu-close-area link-opts]]

      [:div
       primary-opts
       [:div.navbar-menu-items
        [:a.navbar-menu-item (merge link-opts {:href jobsboard-link}) "Jobs"]
        [:hr]
        [:a.navbar-menu-item (merge link-opts {:href issues-link}) "Open Source Issues"]
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

#?(:cljs
   (defn submit-search
     [id]
     (pages/navigate [:jobsboard :query-params {:search (.-value (.getElementById js/document id))}])))

(defn search [default-value query-params]
  [:div.navbar__search
   [:form.navbar-item.is-hidden-mobile
    (merge {:method :get
            :action (routes/path :jobsboard)}
           #?(:cljs {:on-submit (fn [e]
                                  (.preventDefault e)
                                  (submit-search "navbar__search-input"))} ))
    [:input.search {:id "navbar__search-input"
                    :name "search"
                    :type "text"
                    :autoComplete "off"
                    :defaultValue default-value
                    :placeholder "Search for jobs..."}]
    (when default-value
      [:a.a--underlined.clear-search
       (merge {:id "navbar__clear-search-link"
               :href (routes/path :jobsboard)}
              #?(:cljs {:on-click #(set! (.-value (.getElementById js/document "navbar__search-input")))}))
       "Clear search"])
    [:button.search-button {:id "navbar__search-btn"
                            :aria-label "Search button"} [icon "search"]]]
   [:div.navbar-item.is-hidden-desktop
    (interop/multiple-on-click (interop/set-is-open-on-click mobile-search-id true)
                               (interop/set-is-open-on-click menu/logged-in-menu-id false)
                               (interop/set-no-scroll-on-click mobile-search-id true))
    [:button.search-button
     {:id "navbar__search-btn"
      :aria-label "Search button"} [icon "search"]]]])

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
     [icon "search"]]
    [:input.search
     {:id "navbar__mobile-search-input"
      :name "search"
      :type "text"
      :autoComplete "off"
      :defaultValue default-value
      :placeholder "Tap here to type..."}]]])

(defn navbar-end
  [{:keys [logged-in? query-params vertical show-navbar-menu?]}]
  (let [el-id (if logged-in? menu/logged-in-menu-id logged-out-menu-id)
        menu-roll-down [:div.navbar-item.is-hidden-desktop.navbar-item--menu
                        (interop/multiple-on-click (interop/toggle-is-open-on-click el-id)
                                                   (interop/toggle-no-scroll-on-click el-id))
                        [:span "MENU"]]]
    (if logged-in?
      [:div.navbar-search-end-wrapper
       [:div.navbar-search-end.is-hidden-mobile
        [search (:search query-params) query-params]]
       [:div.navbar-end.is-hidden-desktop
        [search (:search query-params) query-params]
        (when show-navbar-menu? menu-roll-down)]]
      [:div.navbar-end
       [:a.navbar-item.navbar-item--login
        (merge
          {:href (routes/path :login :params {:step (if (= "www" vertical) :email :root)})}
          (interop/multiple-on-click (interop/set-is-open-on-click logged-out-menu-id false)
                                     (interop/set-is-open-on-click candidates-overlay-menu-id false)
                                     (interop/disable-no-scroll-on-click)))
        "Login"]
       [:a.navbar-item.is-hidden-mobile.navbar-item--register
        (merge
          {:href (routes/path :get-started)}
          (interop/multiple-on-click (interop/set-is-open-on-click logged-out-menu-id false)
                                     (interop/set-is-open-on-click candidates-overlay-menu-id false)
                                     (interop/disable-no-scroll-on-click)))
        [:button.button
         "Get Started"]]
       (when show-navbar-menu? menu-roll-down)])))

(defn navbar-content
  [{:keys [logged-in? query-params vertical]}]
  (cond
    logged-in? nil
    (= "www" vertical)
    [:div.navbar-items
     [:a.navbar-item.is-hidden-mobile.navbar-item--jobs
      (merge {:href jobsboard-link}
             (interop/set-is-open-on-click candidates-overlay-menu-id false)) "Jobs"]
     [:a.navbar-item.is-hidden-mobile.navbar-item--issues
      (merge {:href issues-link}
             (interop/set-is-open-on-click candidates-overlay-menu-id false)) "Open Source Issues"]
     [:a.navbar-item.is-hidden-mobile.navbar-item--learn
      (merge {:href learn-link}
             (interop/set-is-open-on-click candidates-overlay-menu-id false)) "Blog"]
     [:a.navbar-item.is-hidden-mobile.navbar-item--pricing
      (merge {:href pricing-link}
             (interop/set-is-open-on-click candidates-overlay-menu-id false)) "Pricing"]
     [:div.navbar-item.is-hidden-mobile.navbar-item--candidates
      (interop/toggle-is-open-on-click candidates-overlay-menu-id)
      "Get Hired"]]
    :else
    [:div.navbar-items
     [:a.navbar-item.is-hidden-mobile.navbar-item--jobs
      {:href jobsboard-link} "Jobs"]
     [:a.navbar-item.is-hidden-mobile.navbar-item--issues
      {:href issues-link} "Open Source Issues"]
     [:a.navbar-item.is-hidden-mobile.navbar-item--learn
      {:href learn-link} "Learn"]
     [:a.navbar-item.is-hidden-mobile.navbar-item--hiring
      {:href routes/company-landing-page} "For Employers"]]))

(defn top-bar
  [{:keys [env vertical logged-in? query-params page] :as args}]
  (let [content? (not (contains? routes/no-menu-pages page))]
    [:nav.navbar {:role       "navigation"
                  :aria-label "main navigation"}
     [:div.navbar-item.navbar__logo-container
      [:svg.icon.navbar__logo [icon (if (= "www" vertical) "codi" vertical)]]
      (logo-title vertical env)]
     (when content?
       [navbar-content args])
     (when content?
       [navbar-end args])
     (when (and content? (not logged-in?))
       [logged-out-menu args])
     (when content?
       [candidates-menu args])
     (when content?
       [mobile-search (:search query-params) query-params])]))
