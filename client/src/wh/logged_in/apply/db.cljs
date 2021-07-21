(ns wh.logged-in.apply.db
  (:require [cljs.spec.alpha :as s]
            [wh.job.db :as job]
            [wh.util :as util]))

(def steps #{:step/name :step/email :step/cv-upload :step/thanks :step/current-location
             :step/visa :step/rejection :step/cover-letter :step/skills})

(s/def ::steps-taken (s/coll-of steps))
(s/def ::current-step steps)
(s/def ::reason keyword?)
(s/def ::rejection (s/keys :req-un [::reason]))
(s/def ::id string?)
(s/def ::slug string?)
(s/def ::submit-success? boolean?)
(s/def ::updating? boolean?)
(s/def ::company-managed? boolean?)
(s/def ::company-name string?)
(s/def ::cv-upload-failed? boolean?)
(s/def ::name-update-failed? boolean?)
(s/def ::job (s/or :id (s/keys :req-un [::id])
                   :slug (s/keys :req-un [::slug])))

(s/def ::skills (s/coll-of string?))
(s/def ::skills-update-failed? boolean?)

(s/def ::sub-db (s/keys :opt-un [::job
                                 ::submit-success?
                                 ::updating?
                                 ::current-step
                                 ::rejection
                                 ::steps-taken
                                 ::company-managed?
                                 ::company-name
                                 ::name-update-failed?
                                 ::cv-upload-failed?
                                 ::skills
                                 ::skills-update-failed?]))


(def default-db {::steps-taken #{}})

(defn update-taken-steps [db step]
  (update-in db [::sub-db ::steps-taken] (fnil conj #{}) step))

(defn update-current-step [db step]
  (assoc-in db [::sub-db ::current-step] step))

(defn set-loading [db]
  (assoc-in db [::sub-db ::updating?] true))

(defn unset-loading [db]
  (assoc-in db [::sub-db ::updating?] false))

(defn required-skills [db]
  (map :label (get-in db [::job/sub-db ::job/tags])))

(defn sub-db [db]
  (get db ::sub-db))

(defn selected-skills [sub-db]
  (::skills sub-db))

(defn skill-selected? [skills skill]
  (contains? skills skill))

(defn toggle-skill [sub-db skill]
  (update sub-db ::skills util/toggle skill))

(defn set-skills-update-failed [db]
  (assoc-in db [::sub-db ::skills-update-failed?] true))

(defn unset-skills-update-failed [db]
  (assoc-in db [::sub-db ::skills-update-failed?] false))

(defn skills-update-failed? [db]
  (get-in db [::sub-db ::skills-update-failed?]))

