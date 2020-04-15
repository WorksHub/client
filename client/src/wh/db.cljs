(ns wh.db
  (:require
    [cljs.spec.alpha :as s]
    [wh.interceptors :as interceptors]
    [wh.login.github-callback.db :as github-callback]
    [wh.user.db :as user]))

(s/def ::vertical string?)
(s/def ::platform-name string?)
(s/def ::twitter string?)
(s/def ::default-technologies (s/* string?))
(s/def ::page #{:admin-applications
                :admin-companies
                :admin-company-applications
                :admin-edit-company
                :applied
                :blog
                :candidate
                :candidate-edit-cv
                :candidate-edit-header
                :candidate-edit-private
                :candidates
                :companies
                :company
                :company-applications
                :company-dashboard
                :company-issues
                :company-jobs
                :company-articles
                :contribute
                :contribute-edit
                :create-candidate
                :create-company
                :create-company-offer
                :create-job
                :edit-company
                :edit-job
                :get-started
                :github-callback
                :stackoverflow-callback
                :homepage
                :homepage-company-dashboard
                :homepage-dashboard
                :homepage-not-logged-in
                :how-it-works
                :improve-recommendations
                :issue
                :issues
                :issues-by-language
                :job
                :jobsboard
                :learn
                :learn-by-tag
                :liked
                :login
                :manage-issues
                :manage-repository-issues
                :not-found
                :notifications-settings
                :payment-setup
                :pre-set-search
                :pricing
                :profile
                :profile-edit-company-user
                :profile-edit-cv
                :profile-edit-header
                :profile-edit-private
                :recommended
                :register
                :register-company
                :tags-edit})

;; A mapping of routes to page components. The values can
;; optionally be of the form {:page page, :can-access? boolean-fn},
;; where boolean-fn takes app-db and returns true or false. If it
;; returns false, not-found/page is rendered instead of the page in
;; question.
;; The initial mapping is defined in wh.pages.router.
(s/def :page-mapping/page fn?)
(s/def :page-mapping/can-access? fn?)
(s/def :page-mapping/redirect-fn fn?)
(s/def ::page-mapping (s/map-of ::page
                                (s/or :page-fn fn?
                                      :page-map (s/keys :opt-un [:page-mapping/page :page-mapping/can-access? :page-mapping/redirect-fn]))))

(s/def ::page-params map?) ; arbitrary page parameters, see routes.cljc
(s/def ::query-params (s/map-of string? (s/or :str string?
                                              :coll (s/coll-of string?))))
(s/def ::search-term (s/nilable string?))        ; pre-set search only
(s/def ::loading? boolean?)
(s/def ::tracking-consent? boolean?)
(s/def ::errors (s/coll-of string?))
(s/def ::initial-load? boolean?)
(s/def ::server-side-rendered? boolean?)

;; FIXME: It doesn't contain all sub-dbs yet; we need to verify whether
;; adding all the rest doesn't break anything (and fix the specs if so).
(def sub-dbs
  "Atom holding the list of sub-dbs to verify specs against. Modules
  loaded subsequently can append sub-dbs to here and re-execute
  redefine-app-db-spec! to have their specs checked."
  (atom [::user/sub-db]))

(defn redefine-app-db-spec! []
  (s/def ::app-db
    (s/keys :req (into [::page ::page-mapping ::page-params ::query-params ::vertical ::platform-name ::twitter]
                       @sub-dbs)
            :opt [::loading? ::errors ::default-technologies ::tracking-consent?])))

(redefine-app-db-spec!)

(defn default-db [server-side-db]
  (merge
    {::page                   :homepage
     ::page-params            {}
     ::page-mapping           {}
     ::user/sub-db            user/default-db
     ::github-callback/sub-db github-callback/default-db}
   server-side-db))

(defn blockchain? [vertical]
  (= vertical "blockchain"))

;; note: different for prod and dev builds!
(def default-interceptors interceptors/default-interceptors)

(defn key->id [x]
  (clojure.string/replace (str x) #"[:./]" "_"))

;; These used to live in wh.user.db, but are needed elsewhere.
(defn logged-in? [db]
  (get-in db [:wh.user.db/sub-db :wh.user.db/id]))

(defn has-permission? [db permission]
  (contains? (get-in db [:wh.user.db/sub-db :wh.user.db/company :permissions]) permission))
