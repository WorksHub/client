(ns wh.user.db
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [wh.common.cases :as cases]
    [wh.common.data :as data]
    [wh.common.user :as user]
    [wh.util :as util]))

(defn has-cv? [db]
  (or (get-in db [::sub-db ::cv :link])
      (not (str/blank? (get-in db [::sub-db ::cv :file :url])))))

(defn has-full-name? [db]
  (user/full-name? (get-in db [::sub-db ::name])))

(defn has-current-location? [db]
  (get-in db [::sub-db ::current-location]))

(defn has-visa? [db]
  (get-in db [::sub-db ::visa-status]))

(defn admin-type? [type]
  (= type "admin"))

(defn admin? [db]
  (admin-type? (get-in db [::sub-db ::type])))

(defn company-type? [type]
  (= type "company"))

(defn company? [db]
  (company-type? (get-in db [::sub-db ::type])))

(defn company-id [db]
  (get-in db [::sub-db ::company-id]))

(defn candidate-type? [type]
  (= type "candidate"))

(defn candidate? [db]
  (candidate-type? (get-in db [::sub-db ::type])))

(defn approved? [db]
  (= (get-in db [::sub-db ::approval :status])
     "approved"))

(defn has-permission? [db permission]
  (contains? (get-in db [::sub-db ::company :permissions]) permission))

(defn user-name [db]
  (get-in db [::sub-db ::name]))

(s/def ::id string?)
(s/def ::approved (s/nilable boolean?))
(s/def ::name string?)
(s/def ::email string?)
(s/def ::visa-status (s/coll-of data/visa-options))
(s/def ::visa-status-other (s/nilable string?))
(s/def ::currency (s/nilable (set data/currencies)))
(s/def ::min (s/nilable nat-int?))
(s/def ::time-period (s/nilable (set data/time-periods)))
(s/def ::salary (s/nilable (s/keys :opt-un [::min ::currency ::time-period])))
(s/def ::rating (s/nilable (s/and int? #(<= 1 % 5))))
(s/def ::skill (s/keys :req-un [::name]
                       :opt-un [::rating]))
(s/def ::skills (s/* ::skill))
(s/def ::type string?)
(s/def ::github-id string?)
(s/def :wh.user.cv.file/name (s/nilable string?))
(s/def :wh.user.cv.file/type (s/nilable string?))
(s/def :wh.user.cv.file/url (s/nilable string?))

(s/def :wh.user.cv/link :wh.user.cv.file/url)
(s/def :wh.user.cv/file (s/nilable (s/keys :opt-un [:wh.user.cv.file/type
                                                    :wh.user.cv.file/name
                                                    :wh.user.cv.file/url])))

(s/def ::cv (s/nilable (s/keys :opt-un [:wh.user.cv/file
                                        :wh.user.cv/link])))
(s/def ::consented (s/nilable string?))
(s/def ::saving-consent? boolean?)

(s/def ::company-github-connected? boolean?)

(s/def ::status (s/nilable #{"approved" "rejected" "pending"}))
(s/def ::time (s/nilable string?))
(s/def ::source (s/or :manager string?
                      :other #{"github" "hubspot" "code_riddle" "unknown"}))

(s/def ::approval (s/nilable (s/keys :req-un [::status]
                                     :opt-un [::time
                                              ::source])))

(s/def ::welcome-msgs (s/coll-of string? :kind set?))

(s/def ::sub-db
  (s/keys :opt-un [::id ::name ::visa-status ::visa-status-other
                   ::skills ::salary ::cv ::approval ::github-id ::consented
                   ::saving-consent?]))

(def default-db
  {:welcome-msgs #{}})

;; This is reused between profile and admin/candidate pages.
;; TODO: Currently, profile namespaces all keys in this structure and
;; merges it with sub-db; this is sub-optimal. Refactor it to keep
;; all user info (and only user info) together in a composite data
;; structure.
(defn graphql-user->db
  "Converts GraphQL `User` structure into a form suitable for frontend
  presentation."
  [user]
  (as-> user user
        (cases/->kebab-case user)
        (update user :current-location #(when % (util/namespace-map "location" %)))
        (update user :preferred-locations (partial mapv #(util/namespace-map "location" %)))
        (util/update-in* user [:visa-status] set)
        (update user :role-types set)))

(defn translate-user [user]
  (->> user
       graphql-user->db
       (util/namespace-map "wh.user.db")))
