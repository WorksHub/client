(ns wh.components.navbar.navbar
  (:require #?(:cljs [re-frame.core :refer [dispatch]])
            #?(:cljs [wh.components.navbar.events])
            [wh.common.url :as url]
            [wh.components.attract-card :as attract-card]
            [wh.components.branding :as branding]
            [wh.components.icons :refer [icon]]
            [wh.components.navbar.admin :as admin]
            [wh.components.navbar.candidate :as candidate]
            [wh.components.navbar.company :as company]
            [wh.components.navbar.components :as components]
            [wh.components.navbar.subs :as subs]
            [wh.interop :as interop]
            [wh.re-frame.subs :refer [<sub]]
            [wh.routes :as routes]
            [wh.styles.navbar :as styles]
            [wh.common.user :as user-common]
            [wh.util :as util]))

(defn logo [vertical env]
  [:a {:href      (url/vertical-homepage-href env vertical)
       :data-test "home-link"
       :class     styles/logo__wrapper}
   [icon vertical :class styles/logo__icon]
   [branding/vertical-title vertical {:type :navigation
                                      :size :small}]])

(def search-id "navbar__search-input")

(defn local_search []
  (dispatch [:wh.components.navbar.events/set-local-search  
    (distinct (remove empty? (clojure.string/split (or (.getItem js/localStorage "local_search") "") "||")))]
  )
)

(defn main []
  (local_search)
)

(set! (.-onload js/window) main)


;; Reagent complains about autocomplete keyword
(def autocomplete-k #?(:cljs :autoComplete
                       :clj  :autocomplete))
  
(defn search [type]
  (let [local-search (<sub [::subs/local-search]) search-value (<sub [::subs/search-value]) search-focus (<sub [::subs/search-focus])] 
    
    [:form (merge {:method :get
                   :action (routes/path :search)
                   :class  (util/mc
                             styles/search__wrapper
                             [(= type :small-menu-no-mobile) styles/no-mobile]
                             [(= type :navbar) styles/no-mobile])}
                  #?(:cljs {:on-submit
                            (fn [e]
                              (.preventDefault e)
                              (js/hideMenu)
                              (dispatch [:wh.search/search-with-value search-value]))
                           })
                )
     [icon "search-new" :class styles/search__search-icon]
     [:input (merge
               {:class         (util/mc
                                 styles/search
                                 [(or (= type :small-menu)
                                      (= type :small-menu-no-mobile))
                                  styles/search__small-menu])
                :id            search-id
                :data-test     "job-search"
                :type          "text"
                :name          "query"
                :placeholder   "Search…"
                :value         search-value
                autocomplete-k "off"}
               #?(:cljs {:on-change
                         (fn [e]
                           (dispatch [:wh.components.navbar.events/set-search-value
                                      (.-value (.-target e))]))
                        }
                )
                #?(:cljs {:on-click
                        (fn [e]
                           (dispatch [:wh.components.navbar.events/set-search-focus true]))
                        }
                )
                #?(:cljs {:on-focus
                         (fn [e]
                           (dispatch [:wh.components.navbar.events/set-search-focus true]))
                        }
                )
                #?(:cljs {:on-blur
                        (fn [e]
                          (js/setTimeout (fn [] 
                            (dispatch [:wh.components.navbar.events/set-search-focus false])
                          ) 200)
                        )
                        }
                  )
              )]
                        
     (when search-value
       [:a
        (merge {:href  (routes/path :search)
                :class styles/search__clear-icon}
               #?(:cljs {:on-click
                         #(set! (.-value (.getElementById js/document search-id)))}))
        [icon "close"]]
    )
    
    (when search-focus
       [components/search-dropdown-list 
        (take 5 (filter (fn [a]
            (clojure.string/includes? (clojure.string/lower-case a) (clojure.string/lower-case (or search-value "")))
          ) local-search))
       ]
    )   
  ]))

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
   "Login"])

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

(defn menu-for-mobile-and-tablet [{:keys [candidate? company? admin? logged-in?]}]
  [:div {:class (util/mc styles/small-menu__wrapper)
         :id    "navigation"}
   [close-navigation-button]

   (when-not logged-in?
     [:div (util/smc styles/small-menu__signin)
      [:div (util/smc styles/small-menu__signin-title) "Continue with"]
      [attract-card/signin-buttons]])

   (cond candidate?
         [candidate/candidate-mobile-menu]

         company?
         [company/company-mobile-menu]

         admin?
         [admin/admin-mobile-menu]

         :else [public-mobile-menu])

   [:div (util/smc styles/small-menu__search-wrapper)
    [search :small-menu]]])


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

(defn navbar-right [{:keys [logged-in? content? candidate? company? admin? query-params]}]
  (cond
    logged-in?
    [:div (util/smc styles/navbar__right)
     (when company? [company/notifications {:class styles/no-desktop}])

     [search :small-menu-no-mobile]
     [:div (util/smc styles/navbar__buttons)
      (when candidate? [button-write-article])

      (cond
        company?
        [company/profile-menu]

        candidate?
        [candidate/profile-menu]

        admin?
        [admin/profile-menu])

      [mobile-toggle-navigation-button]]]

    content?
    [:div (util/smc styles/navbar__right)
     [search :navbar]
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
     :page  page}]])


(defn navbar [{:keys [vertical env content? page
                      candidate? company? admin?]
               :as   opts}]
  [:nav {:class     styles/navbar__wrapper
         :data-test "navbar"
         :id         "wh-navbar"
         :role       "navigation"
         :aria-label "main navigation"}
   [:div (util/smc styles/navbar)
    [logo vertical env]

    (when content?
      (cond
        company?
        [company/company-menu opts]

        admin?
        [admin/admin-menu opts]

        candidate?
        [candidate/candidate-menu page]

        :else [public-menu page]))

    [navbar-right opts]]

   [menu-for-mobile-and-tablet opts]])

(defn top-bar
  [{:keys [env vertical logged-in? query-params page user-type] :as _args}]
  (let [candidate? (user-common/candidate-type? user-type)
        company?   (user-common/company-type? user-type)
        admin?     (user-common/admin-type? user-type)
        content?   (not (contains? routes/no-nav-link-pages page))]
    [navbar {:vertical     vertical
             :page         page
             :content?     content?
             :env          env
             :candidate?   candidate?
             :company?     company?
             :admin?       admin?
             :logged-in?   logged-in?
             :query-params query-params}]))
