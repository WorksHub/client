(ns wh.logged-in.apply.db
  (:require
    [cljs.spec.alpha :as s]))

(def step-order
  [:cv :thanks])

(def next-step (zipmap step-order (next step-order)))

(s/def ::step (set step-order))
(s/def ::id string?)
(s/def ::submit-success? boolean?)
(s/def ::loading? boolean?)
(s/def ::cv-upload-failed? boolean?)
(s/def ::job (s/keys :req-un [::id]))

(s/def ::sub-db (s/keys :opt-un [::job ::submit-success? ::loading? ::step ::cv-upload-failed?]))

(def default-db {})
