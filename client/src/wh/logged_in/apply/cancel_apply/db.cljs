(ns wh.logged-in.apply.cancel-apply.db
  (:require  [cljs.spec.alpha :as s]))

;; helper for checking reason
(defn check-reason
  [defaults reason]
  (let [defaults #{"found work" "changed mind"}]
    (if (some #{reason} defaults)
      reason
      (s/conform ::other-reasons reason))))

(def steps #{:reason :thanks})
(def default-reasons #{"found work" "changed mind"})

(s/def ::steps-taken (s/coll-of steps))
(s/def ::current-step steps)
(s/def ::id string?)
(s/def ::slug string?)
(s/def ::submit-success? boolean?)
(s/def ::updating? boolean?)
(s/def ::other-reasons string?)
(s/def ::reason (partial check-reason default-reasons))
(s/def ::reason-failed? boolean?)
(s/def ::job (s/or :id   (s/keys :req-un [::id])
                   :slug (s/keys :req-un [::slug])))

(s/def ::sub-db (s/keys :opt-un [::job
                                 ::submit-success?
                                 ::updating?
                                 ::current-step
                                 ::steps-taken
                                 ::reason ;; should be put in graphql mutation ...
                                 ::reason-failed?
                                 ::cv-upload-failed?]))

(def default-db {::steps-taken #{}})
