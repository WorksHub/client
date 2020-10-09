(ns wh.register.db
  (:require [cljs.spec.alpha :as s]
            [wh.user.db :as user]
            [wh.util :as util]))

(s/def ::name string?)
(s/def ::email string?)
(s/def ::error (s/nilable keyword?))
(s/def ::submitting boolean?)

(s/def ::sub-db (s/keys :req-un [::name
                                 ::email
                                 ::error
                                 ::submitting]))

(defn username [db]
  (get-in db [::user/sub-db ::user/name]))

(defn stackoverflow-signup?
  "checks if user tried to signup with Stack Overflow"
  [db]
  (boolean (and (username db)
                (get-in db [::user/sub-db ::user/stackoverflow-info])
                ;; approval won't be available because user is not created yet
                (not (get-in db [::user/sub-db ::user/approval])))))

(defn twitter-signup?
  "checks if user tried to signup with twitter"
  [db]
  (boolean (and (username db)
                (get-in db [::user/sub-db ::user/twitter-access-token])
                ;; approval won't be available because user is not created yet
                (not (get-in db [::user/sub-db ::user/approval])))))

(defn initialize-sub-db [db]
  (assoc db ::sub-db {:name (if (or (stackoverflow-signup? db)
                                    (twitter-signup? db))
                              (username db)
                              "")
                      :email ""
                      :error nil
                      :submitting false}))

(defn db->sub-db [db]
  (get db ::sub-db))

(defn db->stackoverflow-access-token [db]
  (get-in db [::user/sub-db ::user/stackoverflow-info :access-token]))

(defn db->stackoverflow-account-id [db]
  (get-in db [::user/sub-db ::user/stackoverflow-info :account-id]))

(defn db->twitter-access-token [db]
  (get-in db [::user/sub-db ::user/twitter-access-token]))

(defn db->twitter-account-id [db]
  (get-in db [::user/sub-db ::user/twitter-id]))

(defn db->db-with-user [db graphql-user]
  (update db ::user/sub-db #(merge % (user/translate-user graphql-user))))

(defn ->submitting [sub-db]
  (:submitting sub-db))

(defn ->name [sub-db]
  (:name sub-db))

(defn ->email [sub-db]
  (:email sub-db))

(defn ->error [sub-db]
  (:error sub-db))

(defn set-submitting-flag [db]
  (assoc-in db [::sub-db :submitting] true))

(defn unset-submitting-flag [db]
  (assoc-in db [::sub-db :submitting] false))

(defn set-error [db response]
  (->> response
       util/gql-errors->error-key
       (assoc-in db [::sub-db :error])))

(defn unset-error [db]
  (assoc-in db [::sub-db :error] nil))

(defn set-name [db name]
  (assoc-in db [::sub-db :name] name))

(defn set-email [db email]
  (assoc-in db [::sub-db :email] email))
