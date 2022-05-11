(ns wh.components.navbar.navbar
  (:require #?(:cljs [re-frame.core :refer [dispatch dispatch-sync]])
            #?(:cljs [wh.components.navbar.events])
            [wh.common.url :as url]
            [wh.common.user :as user-common]
            [wh.components.attract-card :as attract-card]
            [wh.components.branding :as branding]
            [wh.components.icons :refer [icon]]
            [wh.components.navbar.admin :as admin]
            [wh.components.navbar.candidate :as candidate]
            [wh.components.navbar.company :as company]
            [wh.components.navbar.components :as components]
            [wh.components.navbar.shared :as navbar-shared]
            [wh.components.navbar.subs :as subs]
            [wh.interop :as interop]
            [wh.re-frame.subs :refer [<sub]]
            [wh.routes :as routes]
            [wh.components.navbar.styles :as styles]
            [wh.util :as util]))

(defn logo [vertical env]
  [:a {:href      (url/vertical-homepage-href env vertical)
       :data-test "home-link"
       :class     styles/logo__wrapper}
   [icon vertical :class styles/logo__icon]
   [branding/vertical-title vertical {:type :navigation
                                      :size :small}]])


;; Reagent complains about autocomplete keyword
(def autocomplete-k #?(:cljs :autoComplete
                       :clj  :autocomplete))

(def search-id-prefix "navbar__search-text-")
(def search-submit-id "navbar__search-submit")

(defn search
  "The universal search box that searches throughout all entity types (jobs, companies, articles, issues).
   Use the `:wh.components.navbar.events/set-search-value` event to update the \"universal-search\" query."
  [_]
  (let [navbar-search-id (str search-id-prefix (util/random-uuid))]
    (fn [type]
      (let [search-value (<sub [::subs/search-value])]
        [:form (merge {;; NB: It was `:get` before, which resulted in automatic query string appending,
                       ;;     thus was dropped in favor of `:post`, which itself was improperly SSR'ed
                       ;;     on a public 'index.html' page as "method=":post" of the <forms>, falling
                       ;;     its submit back to HTTP GET (and an automatically appended query string).
                       :method "post"
                       ;; NB: Only gets called (as an ordinary HTML <form> submit) on a SSR'ed version
                       ;;     of a public page (e.g. 'index.html'). The `:query` path param have to be
                       ;;     appended manually, before triggering the form submit.
                       :action (routes/path :search :params {:query ""})
                       :class  (util/mc
                                 styles/search__wrapper
                                 [(= type :small-menu-no-mobile) styles/no-mobile]
                                 [(= type :navbar) styles/no-mobile])}
                      #?(:cljs {:on-submit
                                (fn [e]
                                  ;; NB: We get the HTML attribute value here intentionally, since user might
                                  ;;     change it via the "search hints" dropdown bypassing the `:on-change`.
                                  (let [search-value (.-value (.getElementById js/document navbar-search-id))]
                                    (dispatch [:wh.search/search-with-value search-value]))
                                  (.preventDefault e))}))
         [icon "search-new" :class styles/search__search-icon]
         [:input (merge
                   {:class         (util/mc
                                     styles/search
                                     [(or (= type :small-menu)
                                          (= type :small-menu-no-mobile))
                                      styles/search__small-menu])
                    :id            navbar-search-id
                    :data-test     "job-search"
                    :type          "text"
                    :name          "search"
                    :placeholder   "Search…"
                    :value         search-value
                    autocomplete-k "off"}
                   (interop/on-search-focus)
                   (interop/on-search-key)
                   (interop/on-search-query-edit)
                   #?(:cljs {:on-change
                             (fn [e]
                               (js/SearchHints.onSearchQueryEdit e)
                               (dispatch-sync [:wh.components.navbar.events/set-search-value
                                               (.-value (.-target e))]))}))]
         #?(:cljs
            ;; NB: A presence of this HTML element is checked in the `search_hints.js` script
            ;;     to find out whether we are on the SSR'ed version of the page (and thus are
            ;;     to submit the HTML <form> directly) or not (and thus are to use this btn).
            [:input.visually-hidden {:id   search-submit-id
                                     :type "submit"}])
         (when-not (empty? search-value)
           [:a
            (merge {:href  (routes/path :search :params {:query ""})
                    :class styles/search__clear-icon}
                   #?(:cljs {:on-click
                             #(set! (.-value (.getElementById js/document navbar-search-id)))}))
            [icon "close"]])]))))

(defn button-write-article []
  [:a {:class     (util/mc styles/button styles/button--contribute)
       :href      (routes/path :contribute)}
   "Write article"])

(defn button-signup []
  [:a {:class     (util/mc styles/button styles/button--signup)
       :href      (routes/path :register)
       :data-test "register"}
   "Sign Up"])

(defn button-employers []
  [:a {:class     (util/mc styles/button styles/button--signup)
       :href      (routes/path :register-company)
       :data-test "for-employers"}
   "For employers"])

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

(defn navbar-right [{:keys [logged-in? content?
                            candidate? company? admin? register?]}]
  (cond
    logged-in?
    [:div (util/smc styles/navbar__right)
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

    (or content? register?)
    [:div (util/smc styles/navbar__right)
     (when-not register? [search :navbar])

     [:div (util/smc styles/navbar__buttons)
      [button-signup]
      [button-signin]
      [button-employers]
      [mobile-toggle-navigation-button]]]

    :else nil))

(defn public-menu [page]
  [:ul (util/smc styles/links)
   [components/link
    {:text  "Jobs"
     :route :jobsboard
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
                      candidate? company? admin? register?]
               :as   opts}]
  [:nav {:class      styles/navbar__wrapper
         :data-test  "navbar"
         :id         "wh-navbar"
         :role       "navigation"
         :aria-label "main navigation"}
   [:div (util/smc styles/navbar)
    [logo vertical env]

    (when (and (not register?) content?)
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
  [{:keys [env vertical logged-in? params query-params page user-type] :as _args}]
  (let [candidate? (user-common/candidate-type? user-type)
        company?   (user-common/company-type? user-type)
        admin?     (user-common/admin-type? user-type)
        content?   (not (contains? routes/no-content page))
        register?  (contains? routes/register-link-pages page)]
    [navbar {:vertical     vertical
             :page         page
             :content?     content?
             :env          env
             :candidate?   candidate?
             :company?     company?
             :admin?       admin?
             :logged-in?   logged-in?
             :params       params
             :query-params query-params
             :register?    register?}]))
