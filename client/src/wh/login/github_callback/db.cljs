(ns wh.login.github-callback.db
  (:require [cljs.spec.alpha :as s]))

(s/def ::error? boolean?)
(s/def ::callback-status #{:sent :fail :success})
(s/def ::sub-db (s/keys :opt [::error? ::callback-status]))
(def default-db {:error? false})
