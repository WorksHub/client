(ns wh.logged-in.apply.db
  (:require [cljs.spec.alpha :as s]))

;; Using namespaced keywords to make them more direct and understandable
(def steps #{:step/name :step/cv-upload :step/thanks :step/current-location
             :step/visa :step/rejection :step/cover-letter})

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
(s/def ::job (s/or :id   (s/keys :req-un [::id])
                   :slug (s/keys :req-un [::slug])))

(s/def ::sub-db (s/keys :opt-un [::job
                                 ::submit-success?
                                 ::updating?
                                 ::current-step
                                 ::rejection
                                 ::steps-taken
                                 ::company-managed?
                                 ::company-name
                                 ::name-update-failed?
                                 ::cv-upload-failed?]))

(def default-db {::steps-taken #{}})
