(ns wh.routes
  (:require
    [#?(:clj  clojure.spec.alpha
        :cljs cljs.spec.alpha) :as s]
    #?(:clj [taoensso.timbre :refer [warn]])
    [bidi.bidi :as bidi]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [wh.common.specs.primitives]
    [wh.common.text :as text]
    [wh.common.url :as url]
    [wh.util :as util]))

(def pages-with-loader #{:homepage
                         :test-cache
                         :learn
                         :learn-search
                         :blog
                         :github-callback
                         :stackoverflow-callback
                         :twitter-callback
                         :liked
                         :recommended
                         :profile
                         :pre-set-search
                         :jobsboard
                         :jobsboard-search
                         :contribute-edit})

(def no-footer-pages #{:register :register-company :payment-setup :login :invalid-magic-link})
(def no-content #{:payment-setup :register :register-company})
(def register-link-pages #{:register :register-company})
(def nextjs-pages #{:series :create-job-new :edit-job-new :conversations :conversation})

;; Here we overwrite the behavior of Bidi's wrt `Pattern` matching with sets.
;; The matching is actually left unchanged from the original implementation
;; and we only deal with the unmatching. In Bidi's original behavior, when
;; multiple `Alternates` are provided, unmatching happens only on the first
;; alternative. We change this behavior to try and see if any of the supplied
;; alternatives has exactly the params that we supply. If we found some, we use
;; the first. If none are found, we revert to the original behavior.
(extend-protocol bidi/Pattern
  #?(:clj  clojure.lang.APersistentSet
     :cljs cljs.core.PersistentHashSet)
  (match-pattern [this s]
    (some #(bidi/match-pattern % s)
          ;; We try to match on the longest string first, so that the
          ;; empty string will be matched last, after all other cases
          (sort-by count > this)))
  (unmatch-pattern [this {:keys [params] :as s}]
    (if-let [match (first (filter (fn [pattern] (= (set (keys params))
                                                   (set (filter keyword? pattern))))
                                  this))]
      (bidi/unmatch-pattern match s)
      (bidi/unmatch-pattern (first this) s))))

;; Bidi route processing works by returning a map that contains
;; :handler and :params.  In WorksHub app, handlers are keywords that
;; denote the page to navigate to, and :params are arbitrary
;; parameters of the page that can be taken from the URL.  However,
;; sometimes there is a need to supply some predefined param right in
;; the route definition.  This isn't allowed out of the box by bidi,
;; so we define our own.

(defn with-params
  "Returns a `Matched` that adds the specified params to the handler."
  [handler & params]
  (let [params-map (apply hash-map params)]
    (reify
      #?(:clj  bidi.bidi.Matched
         :cljs bidi/Matched)
      (resolve-handler [this m]
        (when-let [res (bidi/succeed handler m)]
          (assoc res :params params-map
                     ;; because bidi/make-handler takes :route-params after matching with bidi/match-route*
                     :route-params params-map)))
      (unresolve-handler [this m]
        (when (and (= handler (:handler m))
                   (= params-map (select-keys (:params m) (keys params-map))))
          "")))))

;; The collection routes defined here are supposed to have trailing
;; slashes. If a URL without the trailing slash is requested,
;; there will be a server-side redirect to the correct one.

(defn add-trailing-slashes-to-roots
  [routes]
  (walk/postwalk
    (fn [x]
      (if (and (vector? x)
               (some-> x second map?)
               (some-> x second (get "")))
        (update x 1 #(let [root (get % "")]
                       (assoc % "/" root)))
        x))
    routes))

(def routes
  (add-trailing-slashes-to-roots
    ["/"
     ;;Public SSR Pages - no app.js required
     [["" :homepage]
      ["test-cache" :test-cache]
      ["feed" :feed]
      ["hiring" :employers]
      ["hire-" {[:template] :employers}]
      ["issues" {""                :issues
                 ["/" :company-id] (bidi/tag :issues :issues-for-company-id)}]
      ["issue/" {[:id] :issue}]
      [[[#".+" :language] "-issues"] :issues-by-language]
      ["how-it-works" :how-it-works]
      [#{[:tag "-jobs-in-" :location]
         [:tag "-jobs"]
         ["jobs-in-" :location]} :pre-set-search]
      [[[#".+" :tag] "-articles"] :learn-by-tag]
      ["privacy-policy" :privacy-policy]
      ["terms-of-service" :terms-of-service]
      ["pricing" :pricing]
      ["sitemap" :sitemap]
      ["invalid-magic-link" :invalid-magic-link]
      ["notification-settings" :notification-settings]
      ["metrics" :metrics]
      ;; NB: Simple `["/" :query]` won't work for queries with spaces ("%20" or "+"):
      ;;     `(bidi/match-route routes "/search/Clojure%20Script")` will return `nil`.
      ;;     See 'https://github.com/juxt/bidi/issues/147' for more details.
      ["search" {["/" [#"[^/]*" :query]] :search}]

      ;; Mixed routes
      ["learn" {""                             :learn           ;;Public SSR
                ["/search/" [#"[^/]*" :query]] :learn-search    ;;Public SSR
                "/create"                      :contribute
                "/saved"                       :liked-blogs
                ["/" :id]                      :blog            ;;Public SSR
                ["/" :id "/edit"]              :contribute-edit}]
      ["companies" {""                        :companies        ;;Public SSR
                    "/new"                    :create-company
                    "/applications"           :company-applications
                    ["/" :slug]               :company          ;;Public SSR
                    ["/" :slug "/jobs"]       :company-jobs     ;;Public SSR
                    ["/" :slug "/articles"]   :company-articles ;;Public SSR
                    ["/" :id "/edit"]         :admin-edit-company
                    ["/" :id "/dashboard"]    :company-dashboard
                    ["/" :id "/applications"] :admin-company-applications
                    ["/" :id "/offer"]        :create-company-offer}]
      ["jobs" {""                             :jobsboard        ;;Public SSR
               ["/search/" [#"[^/]*" :query]] :jobsboard-search ;;Public SSR
               "/new"                         :create-job
               ["/" :slug]                    :job              ;;Public SSR
               ["/" :id "/edit"]              :edit-job}]

      ;; Public pages - app.js required
      ["register" :register]
      ["company-registration" :register-company]
      ["login" {""               (with-params :login :step :root)
                "/email"         (with-params :login :step :email)
                "/github"        (with-params :login :step :github)
                "/stackoverflow" (with-params :login :step :stackoverflow)
                "/twitter"       (with-params :login :step :twitter)}]
      ["github-callback" :github-callback]
      ["stackoverflow-callback" :stackoverflow-callback]
      ["twitter-callback" :twitter-callback]

      ;;Private pages - app.js required
      ["admin/" {"companies"  :admin-companies
                 "learn"      :admin-articles
                 "feed"       :feed-preview
                 "promotions" {""                      :promotions-preview
                               ["/new/" :type "/" :id] :create-promotion}
                 "tags"       :tags-edit}]
      ["candidates" {""                        :candidates
                     "/new"                    :create-candidate
                     ["/" :id]                 :candidate
                     ["/" :id "/edit/header"]  :candidate-edit-header
                     ["/" :id "/edit/private"] :candidate-edit-private}]
      ["users/" {[:id] :user}]
      ["company-settings/" :edit-company]
      ["company-issues" {""                                       :company-issues
                         "/repositories"                          :manage-issues
                         ["/repositories/" :owner "/" :repo-name] :manage-repository-issues}]
      ["company-profile" :company-profile]
      ["saved" :liked]
      ["liked" :liked] ;; deprecated
      ["recommended" :recommended]
      ["applied" :applied]
      ["profile" {""              :profile
                  "/edit/header"  :profile-edit-header
                  "/edit/private" :profile-edit-private
                  "/edit"         :profile-edit-company-user
                  ["/" :id]       (bidi/tag :profile :profile-by-id)}]
      ["notifications/" {"settings" :notifications-settings}]
      ["payment/" {"package"  (with-params :payment-setup :step :select-package)
                   "confirm"  (with-params :payment-setup :step :pay-confirm)
                   "complete" (with-params :payment-setup :step :pay-success)}]

      ["tags-collection/" {[:id] :tags-collection}]

      ;; Non UI routes - form submissions
      ["create-company" :create-company-form]
      ["update-notification-settings" :notification-settings-form]

      ;; Non UI routes - redirects, webhooks, API, xml
      ["sitemap.xml" :sitemapxml]
      ["robots.txt" :robots]
      ["rss.xml" :rss]
      ["jooble/" {[:region ".xml"] :jooble}]
      ["magic-link/" {[:token] :magic-link}]
      ["github-dispatch/" {[:board] :github-dispatch}]
      ["stackoverflow-dispatch/" {[:board] :stackoverflow-dispatch}]
      ["twitter-dispatch/" {[:board] :twitter-dispatch}]
      ["github-app-connect" :connect-github-app]
      ["oauth/" {"greenhouse"          :oauth-greenhouse
                 "greenhouse-callback" :oauth-greenhouse-callback
                 "greenhouse-landing"  :oauth-greenhouse-landing
                 "workable"            :oauth-workable
                 "workable-callback"   :oauth-workable-callback
                 "workable-landing"    :oauth-workable-landing
                 "slack"               :oauth-slack
                 "slack-callback"      :oauth-slack-callback
                 "slack-landing"       :oauth-slack-landing
                 "twitter"             :oauth-twitter}]
      ["documents/" {[:access-id "/" :doc-type] :document-access}]
      ["api/" [["graphql" :graphql]
               ["graphql-schema" :graphql-schema]
               ["webhook/" {"github-app" :github-app-webhook}]
               ["analytics" :analytics]
               ["login-as" :login-as]
               ["logout" :logout]
               ["image" :image-upload]
               ["cv" {""                           :cv-upload
                      ["/" :filename]              :cv-file
                      ["/" :user-id "/" :filename] :cv-file-legacy}]
               ["cover-letter" {""              :cover-letter-upload
                                ["/" :filename] :cover-letter-file}]
               ["reset-fixtures" :reset-fixtures]
               ["admin" {""             :admin-command-list
                         ["/" :command] :admin-command}]
               ["prospect" :prospect]
               ["updates/token" :updates-token]]]
      ["health/" {[:commit-sha] :health-by-commit-sha}]
      ;; next js routes
      ["series" :series]
      ["jobs-new" {"/create"           :create-job-new
                   ["/" :slug "/edit"] :edit-job-new}]
      ["conversations" {""        :conversations
                        ["/" :id] :conversation}]]]))

;;TODO this config should pulled partially from wh.response.ssr/page-content map
(def server-side-only-pages #{:employers
                              :invalid-magic-link
                              :notification-settings
                              :not-found
                              :privacy-policy
                              :sitemap
                              :terms-of-service
                              :metrics
                              :oauth-greenhouse
                              :oauth-slack
                              :oauth-workable})
;;TODO this config should be added to wh.response.ssr/page-content map
(def pages-without-app-js-when-not-logged-in #{:blog
                                               :company
                                               :companies
                                               :company-jobs
                                               :company-articles
                                               :employers
                                               :feed
                                               :homepage
                                               :test-cache
                                               :how-it-works
                                               ;;:issue CH3610
                                               ;;:issues CH3615
                                               :job
                                               :jobsboard
                                               :jobsboard-search
                                               :learn
                                               :learn-search
                                               :learn-by-tag
                                               :pre-set-search
                                               :user})
                                               ;;:pricing CH3618

(defn prepare-path-param [param]
  (if (some? param)
    ;; NB: Probably there is a more elegant way
    ;; of achieving this w/ `bidi` protocols,
    ;; but we don't care much anymore...
    (if (string? param)
      (-> (str/replace param "+" " ")
          (bidi/url-encode)
          (str/replace "%20" "+"))
      param)
    ""))

(defn prepare-path-params [m]
  (->> m
       (util/map-vals prepare-path-param)
       seq
       flatten))

(s/fdef prepare-path-params
  :args (s/cat :m :http.path/params)
  :ret (s/coll-of (s/or :kwd keyword? :str string?)))

(def default-path "")

(def ^:private path-error-msg "Unable to construct URI for '%s' handler and '%s' opts.")

(defn path
  "Constructs a URI for a particular web `handler` and kwargs `opts` which may include
   `params` (path variables), `query-params` (parsed query string) or `anchor` (fragment)."
  [handler & {:keys [params query-params anchor] :as opts}]
  (try
    (cond-> (if handler
              (apply bidi/path-for routes handler (prepare-path-params params))
              default-path)
            (seq query-params)      (str "?" (url/serialize-query-params query-params))
            (text/not-blank anchor) (str "#" anchor))
    (catch #?(:clj Exception :cljs js/Error) _
      (let [message (text/format path-error-msg handler opts)]
        #?(:clj  (warn message)
           :cljs (js/console.warn message)))
      default-path)))

(s/fdef path
  :args (s/cat :handler keyword?
               :kwargs (s/keys* :opt-un [:http.path/params
                                         :http/query-params]))
  :ret string?)

(s/fdef bidi/path-for
  :args (s/cat :routes vector?
               :handler keyword?)
  :ret string?)

(def server-side-only-paths (set (map path server-side-only-pages)))

(defn handler->name [handler]
  (when handler
    (->> (str/split (name handler) #"-")
         (map str/capitalize)
         (str/join " "))))
