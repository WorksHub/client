(ns wh.components.error.db
  (:require [cljs.spec.alpha :as s]))


(s/def ::message string?)
(s/def ::type #{:error :success})
(s/def ::retry-event vector?)

(s/def ::sub-db (s/keys :opt [::retry-event
                              ::message]))

(def default-db {})

