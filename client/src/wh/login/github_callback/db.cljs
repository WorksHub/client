(ns wh.login.github-callback.db
  (:require [cljs.spec.alpha :as s]))

(s/def ::github-error? boolean?)

(s/def ::sub-db (s/keys :opt [::github-error?]))

(def default-db {::github-error? false})
