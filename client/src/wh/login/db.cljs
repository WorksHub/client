(ns wh.login.db
  (:require [cljs.reader :as r]
            [bidi.bidi :as bidi]
            [goog.Uri :as uri]
            [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [wh.common.specs.primitives :as primitives]
            [wh.db :as db]
            [wh.routes :as routes]))

(s/def ::email string?)
(s/def ::redirect (s/coll-of any?))
(s/def ::error #{:email-not-sent :unknown-error :no-user-found-with-email :invalid-arguments :email-unsuccessful})
(s/def ::submitting boolean?)
(s/def ::sub-db (s/keys :req-un [::submitting]
                        :opt-un [::error
                                 ::email
                                 ::redirect]))

(def default-db {:submitting false})

;; ------------------------------------

(defn is-step? [db step]
  (= step (get-in db [::db/page-params :step])))

(defn is-prod-env? [db]
  (= (:wh.settings/environment db) :prod))

(defn email-sent? [db]
  (get-in db [::db/query-params "sent"]))

;; ------------------------------------

(defn query-redirect->path
  [query-redirect]
  (when-let [route (->> query-redirect
                        (bidi/url-decode)
                        (bidi/match-route routes/routes))]
    (let [query-params (let [params (-> query-redirect uri/parse .getQueryData)]
                         (zipmap (.getKeys params) (.getValues params)))]
      ;; url params can be 'route-params' or just 'params' depending on how
      ;; they are specified in routes (see ':company' vs ':payment-setup')
      (cond-> (vector (:handler route))
              (or (not-empty (:route-params route))
                  (not-empty (:params route)))
              (concat [:params (merge (:route-params route)
                                      (:params route))])
              (not-empty query-params)
              (concat [:query-params query-params])))))

(defn construct-redirect [path]
  (some->> path
           (apply routes/path)))

(defn redirect-vector [db]
  "returns a vector description of redirect"
  (try (let [redirect-path-query       (some-> (get-in db [:wh.db/query-params "redirect"])
                                               query-redirect->path)
             redirect-path-cache       (some-> (js/popAuthRedirect)
                                               r/read-string)
             redirect-path-current-url (js/popAuthRedirectCurrentUrl)
             redirect-path-db          (get-in db [::sub-db :redirect])]
         (or (-> (or redirect-path-cache redirect-path-query redirect-path-db)
                 construct-redirect)
             redirect-path-current-url))
       (catch js/Error e
         (do (js/console.error e)
             nil))))

(defn redirect-url
  "builds a redirect url to pass to the BE"
  [db]
  (redirect-vector db))

(defn redirect-post-login-or-registration [db]
  [(into [:wh.events/nav] (or (redirect-vector db) [:homepage]))])

(defn error [db]
  (get-in db [::sub-db :error]))

(defn set-email [db email]
  (assoc db :email email))

(defn set-password [db email]
  (assoc db :password email))

(defn email [db]
  (some->> (get-in db [::sub-db :email])
           str/trim))

(defn password [db]
  (some->> (get-in db [::sub-db :password])
           str/trim))

(defn set-error [db error]
  (assoc-in db [::sub-db :error] error))

(defn valid-email? [email]
  (s/valid? ::primitives/email email))

(defn set-submitting [db]
  (assoc-in db [::sub-db :submitting] true))

(defn unset-submitting [db]
  (assoc-in db [::sub-db :submitting] false))

(defn submitting? [db]
  (get-in db [::sub-db :submitting]))

(def status->error
  {:email-not-sent           "Email was not sent, use whitelisted emails in development and staging environment."
   :invalid-arguments        "Please ensure you have supplied a valid email address."
   :no-user-found-with-email "There is no account associated with this email.
                               If you are a company user,
                               please contact your company manager or talk to us via live chat."
   :unknown-error            "There was an error processing the request. Please try again later."
   :email-unsuccessful       "There was an problem with sending a login email. Please try again later."})
