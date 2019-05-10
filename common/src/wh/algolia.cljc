(ns wh.algolia
  (:require [#?(:clj clojure.spec.alpha
                :cljs cljs.spec.alpha) :as s]))

(defn get-search-url
  "Algolia recommends to re-try other hostnames in case of error in order to guarantee high availability.
https://places-dsn.algolia.net
https://places-1.algolianet.com
https://places-2.algolianet.com
https://places-3.algolianet.com."
  ([domain-prefix]
   (str "https://" domain-prefix "-dsn.algolia.net"))
  ([domain-prefix retry-attempt]
   (if-not retry-attempt
     (str "https://" domain-prefix "-dsn.algolia.net")
     (str "https://" domain-prefix "-" retry-attempt ".algolianet.com"))))

(s/def :algolia.search-filters/key
  #{"location.city"
    "location.country"
    "location.country-code"
    "location.region"
    "manager"
    "published"
    "remote"
    "remuneration.currency"
    "remuneration.time-period"
    "remuneration.min"
    "role-type"
    "sponsorship-offered"
    "tags"
    "verticals"
    "wh-region"})

(s/def :algolia.search-filters/value string?)
(s/def :algolia.search-filters/kv (s/tuple :algolia.search-filters/key :algolia.search-filters/value))

(s/def :algolia/filters (s/coll-of :algolia.search-filters/kv))
