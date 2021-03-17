(ns wh.routes
  (:require
    #?(:clj [clojure.spec.alpha :as s])
    #?(:clj [ring.util.codec :as codec])
    #?(:clj [taoensso.timbre :refer [warn]])
    #?(:cljs [cljs.spec.alpha :as s])
    #?(:cljs [goog.Uri.QueryData :as query-data])
    [bidi.bidi :as bidi]
    [clojure.string :as str]
    [wh.common.specs.primitives]
    [wh.common.text :as text]))

(def company-landing-page "https://www.works-hub.com")
(def pages-with-loader #{:homepage
                         :learn
                         :blog
                         :github-callback
                         :stackoverflow-callback
                         :twitter-callback
                         :liked
                         :recommended
                         :profile
                         :pre-set-search
                         :jobsboard
                         :contribute-edit})

(def no-footer-pages #{:register :register-company :payment-setup :get-started :login :invalid-magic-link})
(def no-nav-link-pages #{:register :register-company :payment-setup :get-started})

;; Here we overwrite the behavior of Bidi's wrt Pattern matching with sets.
;; The matching is actually left unchanged from the original implementation
;; and we only deal with the unmatching. In Bidi's original behavior, when
;; multiple Alternates are provided, unmatching happens only on the first
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
  "Returns a Matched that adds the specified params
  to the handler."
  [handler & params]
  (let [params-map (apply hash-map params)]
    (reify
      #?(:clj  bidi.bidi.Matched
         :cljs bidi/Matched)
      (resolve-handler [this m]
        (when-let [res (bidi/succeed handler m)]
          (assoc res :params params-map)))
      (unresolve-handler [this m]
        (when (and (= handler (:handler m))
                   (= params-map (select-keys (:params m) (keys params-map))))
          "")))))

;; The collection routes defined here are supposed to have trailing
;; slashes. If a URL without the trailing slash is requested,
;; there will be a server-side redirect to the correct one.

(def routes ["/"
             ;;Public SSR Pages - no app.js required
             [["" :homepage]
              ["feed" :feed]
              ["hiring" :employers]
              ["hire-" {[:template] :employers}]
              ["issues/" {""            :issues
                          [:company-id] (bidi/tag :issues :issues-for-company-id)}]
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
              ["metrics" :metrics]
              ["search" :search]

              ;; Mixed routes
              ["learn/" {""            :learn ;;Public SSR
                         "create"      :contribute
                         [:id]         :blog  ;;Public yet to be SSR CH2655
                         [:id "/edit"] :contribute-edit}]
              ["companies/" {""                    :companies        ;;Public SSR
                             "new"                 :create-company
                             "applications"        :company-applications
                             [:slug]               :company          ;;Public SSR
                             [:slug "/jobs"]       :company-jobs     ;;Public SSR
                             [:slug "/articles"]   :company-articles ;;Public SSR
                             [:id "/edit"]         :admin-edit-company
                             [:id "/dashboard"]    :company-dashboard
                             [:id "/applications"] :admin-company-applications
                             [:id "/offer"]        :create-company-offer}]
              ["jobs/" {""            :jobsboard ;;Public SSR
                        "new"         :create-job
                        [:slug]       :job       ;;Public SSR
                        [:id "/edit"] :edit-job}]

              ;; Public pages - app.js required
              ["register" :register]
              ["company-registration" :register-company]
              ["login" {""               (with-params :login :step :root)
                        "/"              (with-params :login :step :root)
                        "/email"         (with-params :login :step :email)
                        "/github"        (with-params :login :step :github)
                        "/stackoverflow" (with-params :login :step :stackoverflow)
                        "/twitter"       (with-params :login :step :twitter)}]
              ["get-started" :get-started]
              ["github-callback" :github-callback]
              ["stackoverflow-callback" :stackoverflow-callback]
              ["twitter-callback" :twitter-callback]

              ;;Private pages - app.js required
              ["admin/" {"companies"  :admin-companies
                         "learn"      :admin-articles
                         "feed"       :feed-preview
                         "promotions" {""                      :promotions-preview
                                       ["/new/" :type "/" :id] :create-promotion}}]
              ["candidates/" {""                    :candidates
                              "new"                 :create-candidate
                              [:id]                 :candidate
                              [:id "/edit/header"]  :candidate-edit-header
                              [:id "/edit/private"] :candidate-edit-private}]
              ["users/" {[:id] :user}]
              ["company-settings/" :edit-company]
              ["company-issues/" {""                                      :company-issues
                                  "repositories"                          :manage-issues
                                  ["repositories/" :owner "/" :repo-name] :manage-repository-issues}]
              ["saved" :liked]
              ["liked" :liked] ;; deprecated
              ["recommended" :recommended]
              ["applied" :applied]
              ["profile/" {""             :profile
                           "edit/header"  :profile-edit-header
                           "edit/private" :profile-edit-private
                           "edit"         :profile-edit-company-user
                           [:id]          (bidi/tag :profile :profile-by-id)}]
              ["notifications/" {"settings" :notifications-settings}]
              ["payment/" {"package"  (with-params :payment-setup :step :select-package)
                           "confirm"  (with-params :payment-setup :step :pay-confirm)
                           "complete" (with-params :payment-setup :step :pay-success)}]
              ["tags" {"/edit" :tags-edit}]

              ["tags-collection/" {[:id] :tags-collection}]

              ;; Non UI routes - form submissions
              ["create-company" :create-company-form]

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
                       ["admin/" {""         :admin-command-list
                                  [:command] :admin-command}]
                       ["prospect" :prospect]]]
              ["health/" {[:commit-sha] :health-by-commit-sha}]]])

;;TODO this config should pulled partially from wh.response.ssr/page-content map
(def server-side-only-pages #{:employers
                              :invalid-magic-link
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
                                               :how-it-works
                                               ;;:issue CH3610
                                               ;;:issues CH3615
                                               :job
                                               :jobsboard
                                               :learn
                                               :learn-by-tag
                                               :pre-set-search
                                               :user})
                                               ;;:pricing CH3618

(def server-side-only-paths (set (map #(bidi/path-for routes %) server-side-only-pages)))

(defn serialize-query-params
  "Serializes a map as query params string."
  [m]
  #?(:clj
     (codec/form-encode m))
  #?(:cljs
     (let [usp (js/URLSearchParams.)]
       (run! (fn [[k v]]
               (if (coll? v)
                 (run! (fn [v'] (.append usp (name k) v')) v)
                 (.append usp (name k) v))) m)
       (.toString usp))))

(s/fdef serialize-query-params
        :args (s/cat :m :http/query-params)
        :ret string?)

(defn path [handler & {:keys [params query-params anchor] :as opts}]
  (try
    (cond->
      (when handler (apply bidi/path-for routes handler (flatten (seq params))))
      (seq query-params) (str "?" (serialize-query-params query-params))
      (text/not-blank anchor) (str "#" anchor))
    (catch #?(:clj Exception) #?(:cljs js/Object) _
                                                  (let [message (str "Unable to construct link: " (pr-str (assoc opts :handler handler)))]
                                                    #?(:clj (warn message))
                                                    #?(:cljs (js/console.warn message)))
                                                  "")))


(s/fdef path
        :args (s/cat :handler keyword?
                     :kwargs (s/keys* :opt-un [:http.path/params
                                               :http/query-params]))
        :ret string?)

(s/fdef bidi/path-for
        :args (s/cat :routes vector?
                     :handler keyword?)
  :ret string?)

(defn handler->name [handler]
  (when handler
    (->> (str/split (name handler) #"-")
         (map str/capitalize)
         (str/join " "))))
