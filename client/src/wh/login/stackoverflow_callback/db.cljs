(ns wh.login.stackoverflow-callback.db
  (:require [cljs.spec.alpha :as s]))

(s/def ::error? boolean?)
(s/def ::sub-db (s/keys :opt [::error?]))
(def default-db {::error? false})
