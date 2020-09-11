(ns wh.login.db
  (:require [cljs.spec.alpha :as s]
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

(defn is-dev-env? [db]
  (and (= (:wh.settings/environment db) :dev)
       (not (get-in db [::db/query-params "force-email"]))))

(defn email-sent? [db]
  (get-in db [::db/query-params "sent"]))

;; ------------------------------------

(defn redirect-post-login-or-registration [db]
  [(into [:wh.events/nav] (get-in db [::sub-db :redirect] [:homepage]))])

(defn error [db]
  (get-in db [::sub-db :error]))

(defn set-email [db email]
  (assoc db :email email))

(defn email [db]
  (some->> (get-in db [::sub-db :email])
           str/trim))

(defn redirect-link [db]
  (when-let [path (get-in db [::sub-db :redirect])]
    (apply routes/path path)))

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
