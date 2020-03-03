(ns wh.company.dashboard.db
  (:require [cljs.spec.alpha :as s]))

(s/def ::publishing-jobs (s/coll-of string? :kind set))
(s/def ::publish-celebrations (s/coll-of string? :kind set))

(defn initial-db
  [db]
  (merge
    {::error                nil
     ::activity-items-count 15
     ::publishing-jobs      #{}
     ::publish-celebrations #{}}
    db))
