(ns wh.user.db
  (:require [cljs-time.coerce :as tc]
            [cljs-time.core :as t]
            [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [wh.common.cases :as cases]
            [wh.common.data :as data]
            [wh.common.keywords :as keywords]
            [wh.common.user :as user]
            [wh.util :as util]))

(defn update-visa-status [db visa-status]
  (assoc-in db [::sub-db ::visa-status] visa-status))

(defn update-visa-status-other [db visa-status-other]
  (assoc-in db [::sub-db ::visa-status-other] visa-status-other))

(defn update-name [db name]
  (assoc-in db [::sub-db ::name] name))

(defn update-cv [db file]
  (assoc-in db [::sub-db ::cv :file] file))

(defn update-skills [db skills]
  (assoc-in db [::sub-db ::skills] skills))

(defn id [db]
  (get-in db [::sub-db ::id]))

(defn published? [db]
  (boolean (get-in db [::sub-db ::published])))

(defn toggle-published [db]
  (update-in db [::sub-db ::published] not))

(defn has-skills? [db]
  (seq (get-in db [::sub-db ::skills])))

(defn has-cv? [db]
  (or (get-in db [::sub-db ::cv :link])
      (not (str/blank? (get-in db [::sub-db ::cv :file :url])))))

(defn has-full-name? [db]
  (user/full-name? (get-in db [::sub-db ::name])))

(defn has-current-location? [db]
  (get-in db [::sub-db ::current-location]))

(defn has-visa? [db]
  (not (empty? (get-in db [::sub-db ::visa-status]))))

(defn company-id [db]
  (get-in db [::sub-db ::company-id]))

(defn approved? [db]
  (= (get-in db [::sub-db ::approval :status])
     "approved"))

(defn user-type [db]
  (get-in db [::sub-db ::type]))

(defn company-type? [type]
  (= type "company"))

(defn company? [db]
  (company-type? (user-type db)))

(defn admin-type? [type]
  (= type "admin"))

(defn admin? [db]
  (admin-type? (user-type db)))

(defn has-permission? [db permission]
  (contains? (get-in db [::sub-db ::company :permissions]) permission))

(defn owner?
  [db id]
  (= (get-in db [::sub-db ::company-id]) id))

(defn owner-by-slug?
  [db slug]
  (= (get-in db [::sub-db ::company :slug]) slug))

(defn user-name [db]
  (get-in db [::sub-db ::name]))

(defn old-enough?
  [user num-days]
  (when-let [created (::user/created user)]
    (let [created (tc/to-date-time created)
          now (t/now)]
      (t/before? created (t/minus now (t/days num-days))))))

(defn onboarding-msg-not-seen?
  [db msg]
  (let [user (::sub-db db)
        num-days ({"jobs" 7, "blogs" 7} msg)]
    (not (or (contains? (::onboarding-msgs user) msg)
             (when num-days (old-enough? user num-days))))))

(defn company-onboarding-msg-not-seen?
  [db msg]
  (let [company (get-in db [::sub-db ::company])]
    (not (contains? (:onboarding-msgs company) msg))))

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
(def min-experience 0)
(def max-experience 11)
(s/def ::rating (s/nilable (s/int-in min-experience (inc max-experience))))
(s/def ::skill (s/keys :req-un [::name]
                       :opt-un [::rating]))
(s/def ::skills (s/* ::skill))
(s/def ::type string?)
(s/def ::github-id (s/nilable string?))
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

(s/def ::onboarding-msgs (s/coll-of string? :kind set?))

(s/def ::sub-db
  (s/keys :opt-un [::id ::name ::visa-status ::visa-status-other
                   ::skills ::salary ::cv ::approval ::github-id ::consented
                   ::saving-consent?]))

(def default-db
  {:onboarding-msgs #{}})

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
        (update user :current-location #(when % (keywords/namespace-map "location" %)))
        (update user :preferred-locations (partial mapv #(keywords/namespace-map "location" %)))
        (util/update* user :visa-status set)
        (update user :role-types set)))

(defn translate-user [user]
  (->> user
       graphql-user->db
       (keywords/namespace-map "wh.user.db")))
