(ns wh.routes
  (:require
    #?(:clj [clojure.spec.alpha :as s])
    #?(:clj [ring.util.codec :as codec])
    #?(:clj [taoensso.timbre :refer [warn]])
    #?(:cljs [cljs.spec.alpha :as s])
    #?(:cljs [goog.Uri.QueryData :as query-data])
    [bidi.bidi :as bidi]
    [wh.common.specs.primitives]))

(def company-landing-page "https://www.works-hub.com")
(def pages-with-loader #{:homepage :learn :blog :github-callback :liked :recommended :profile :pre-set-search :jobsboard :contribute-edit})
(def no-menu-pages #{:register :register-company :payment-setup :get-started})
(def no-footer-pages (set (concat no-menu-pages #{:blog :job})))

;; Here we overwrite the behavior of Bidi's wrt Pattern matching with sets.
;; The matching is actually left unchanged from the original implementation
;; and we only deal with the unmatching. In Bidi's original behavior, when
;; multiple Alternates are provided, unmatching happens only on the first
;; alternative. We change this behavior to try and see if any of the supplied
;; alternatives has exactly the params that we supply. If we found some, we use
;; the first. If none are found, we revert to the original behavior.
(extend-protocol bidi/Pattern
  #?(:clj clojure.lang.APersistentSet
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
      #?(:clj bidi.bidi.Matched
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

(def routes ["/" [["" :homepage]
                  ["hire-" {[:template] :homepage}]
                  ["register/" {"name"         (with-params :register :step :name)
                                "thanks"       (with-params :register :step :thanks)
                                "skills"       (with-params :register :step :skills)
                                "company-info" (with-params :register :step :company-info)
                                "company"      (with-params :register :step :company)
                                "location"     (with-params :register :step :location)
                                "verify"       (with-params :register :step :verify)
                                "test"         (with-params :register :step :test)
                                "email"        (with-params :register :step :email)}]
                  ["candidates/" {""                    :candidates
                                  "new"                 :create-candidate
                                  [:id]                 :candidate
                                  [:id "/edit/header"]  :candidate-edit-header
                                  [:id "/edit/cv"]      :candidate-edit-cv
                                  [:id "/edit/private"] :candidate-edit-private
                                  }]
                  ["company-settings/" :edit-company]
                  ["issues/" {""            :issues
                              [:company-id] (bidi/tag :issues :issues-for-company-id)}]
                  ["issue/" {[:id] :issue}]
                  ["company-issues/" {""       :company-issues
                                      "manage" :manage-issues}]
                  ["dashboard" :homepage-dashboard]
                  ["how-it-works" :how-it-works]
                  ["companies/" {""    :companies
                                 "new" :create-company
                                 "applications" :company-applications
                                 [:id] :company ; reserved for public company page
                                 [:id "/edit"] :admin-edit-company
                                 [:id "/dashboard"] :company-dashboard
                                 [:id "/applications"] :admin-company-applications
                                 [:id "/offer"] :create-company-offer
                                 "register/" {"details"  (with-params :register-company :step :company-details)
                                              "job"      (with-params :register-company :step :job-details)
                                              "complete" (with-params :register-company :step :complete)}}]
                  ["liked" :liked]
                  ["recommended" :recommended]
                  [#{[:tag "-jobs-in-" :location]
                     [:tag "-jobs"]
                     ["jobs-in-" :location]} :pre-set-search]
                  ["jobs/" {""            :jobsboard
                            "new"         :create-job
                            [:id]         :job
                            [:id "/edit"] :edit-job}]
                  ["profile/" {""             :profile
                               "edit/header"  :profile-edit-header
                               "edit/cv"      :profile-edit-cv
                               "edit/private" :profile-edit-private
                               "edit"         :profile-edit-company-user}]
                  ["notifications/" {"settings" :notifications-settings}]
                  ["improve-recommendations" :improve-recommendations]
                  ["learn/" {""                         :learn
                             "create"                   :contribute
                             [[#".+" :tag] "-articles"] :learn-by-tag ;; necessary as they are user generated strings and thus need to be encoded
                             [:id]                      :blog
                             [:id "/edit"]              :contribute-edit}]
                  ["privacy-policy" :privacy-policy]
                  ["terms-of-service" :terms-of-service]
                  ["pricing" :pricing]
                  ["payment/" {"package"  (with-params :payment-setup :step :select-package)
                               "confirm"  (with-params :payment-setup :step :pay-confirm)
                               "complete" (with-params :payment-setup :step :pay-success)}]
                  ["login" {""        (with-params :login :step :root)
                            "/"       (with-params :login :step :root)
                            "/email"  (with-params :login :step :email)
                            "/github" (with-params :login :step :github)}]
                  ["get-started" :get-started]
                  ["magic-link/" {[:token] :magic-link}]
                  ["invalid-magic-link" :invalid-magic-link]
                  ["github-callback" :github-callback]
                  ["github-dispatch/" {[:user-type "/" :board] :github-dispatch}]
                  ["sitemap" :sitemap]
                  ["sitemap.xml" :sitemapxml]
                  ["rss.xml" :rss]
                  ["data" :data-page]
                  ["oauth/" {"greenhouse" :oauth-greenhouse
                             "greenhouse-callback" :oauth-greenhouse-callback
                             "slack" :oauth-slack
                             "slack-callback" :oauth-slack-callback
                             "process"    :oauth-process}]
                  ["api/" [["graphql" :graphql]
                           ["graphql-schema" :graphql-schema]
                           ["gh-webhook" :gh-webhook]
                           ["webhook/" {"mailchimp" :mailchimp-webhook}]
                           ["analytics" :analytics]
                           ["login-as" :login-as]
                           ["logout" :logout]
                           ["image" :image-upload]
                           ["cv" {""                           :cv-upload
                                  ["/" :filename]              :cv-file
                                  ["/" :user-id "/" :filename] :cv-file-legacy}]
                           ["reset-fixtures" :reset-fixtures]
                           ["admin/" {[:command] :admin-command}]]]]])

(def server-side-only-pages #{:sitemap :oauth-greenhouse :oauth-slack :privacy-policy :not-found})
(def pages-without-app-js-when-not-logged-in #{:homepage :job :jobsboard :pre-set-search :learn :learn-by-tag})
(def server-side-only-paths (set (map #(bidi/path-for routes %) server-side-only-pages)))

(defn serialize-query-params
  "Serializes a map as query params string."
  [m]
  #?(:clj
     (codec/form-encode m))
  #?(:cljs
     (query-data/createFromKeysValues (clj->js (keys m))
                                      (clj->js (vals m)))))
(s/fdef serialize-query-params
  :args (s/cat :m :http/query-params)
  :ret string?)

(defn path [handler & {:keys [params query-params]}]
  (try
    (cond->
      (apply bidi/path-for routes handler (flatten (seq params)))
      (seq query-params) (str "?" (serialize-query-params query-params)))
    (catch #?(:clj Exception) #?(:cljs js/Object) _
      (let [message (str "Unable to construct link: " (pr-str (assoc params :handler handler)))]
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
