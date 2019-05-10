(ns wh.company.candidate.db
  (:require [cljs.spec.alpha :as s]
            [wh.logged-in.profile.db :as profile]))

(s/def ::data
  (s/keys :opt-un [::profile/image-url
                   ::profile/email
                   ::profile/preferred-locations
                   ::profile/name
                   ::profile/other-urls
                   ::profile/summary
                   ::profile/skills
                   ::profile/company-perks]))

(s/def ::job-titles (s/map-of string? string?))

(s/def ::updating? boolean?)

(s/def ::sub-db (s/keys :req [::data
                              ::updating?]
                        :opt [::job-titles]))

(def default-db {::data {}
                 ::updating? false})
