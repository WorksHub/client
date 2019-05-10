(ns wh.login.db
  (:require [cljs.spec.alpha :as s]))

(s/def ::magic-email string?)

(s/def ::redirect (s/coll-of any?))

(s/def ::magic-status #{:not-posted :email-not-sent :unknown-error :success :no-user-found-with-email :invalid-arguments :email-unsuccessful})

(s/def ::sub-db (s/keys :req [::magic-status]
                        :opt [::magic-email ::redirect]))

(def default-db {::magic-status :not-posted})

(defn redirect-post-login-or-registration [db]
  [(into [:wh.events/nav] (get-in db [::sub-db ::redirect] [:homepage]))])
