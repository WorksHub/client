(ns wh.components.auth-popup.db
  (:require [cljs.spec.alpha :as s]))

(s/def ::visible? boolean?)
(s/def ::type #{:issue :homepage-jobcard-apply :homepage-jobcard-more-info :jobcard-apply :jobpage-see-more :jobpage-apply :upvote :homepage-contribute})
(s/def ::context (s/keys :opt-un [::type]))

(s/def ::sub-db (s/keys :opt [::visible? ::context]))

(def default-db {})
