(ns wh.logged-in.dashboard.db
  (:require [cljs.spec.alpha :as s]))

(s/def ::sub-db (s/keys))

(def default-db
  {::hidden-onboarding-msgs #{}})
