(ns wh.logged-in.profile.db
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [wh.common.cases :as cases]
            [wh.util :as util]))

(s/def ::predefined-avatar integer?)
(s/def ::custom-avatar-mode boolean?)
(s/def ::editing (s/nilable keyword?))
(s/def ::contributions (s/* (s/keys :req-un [::id ::title]
                                    :opt-un [::published])))
;; FIXME: This spec is wrong (:wh/location specs unnamespaced keys and
;; we expect namespaced ones); change that so we can reuse the
;; location spec between client and server.
;; (s/def ::location-suggestions (s/map-of integer? (s/nilable (s/coll-of :wh/location))))

(s/def ::sub-db (s/keys :req [::editing ::predefined-avatar ::custom-avatar-mode]
                        :opt [::contributions]))

(def default-db {::editing              nil
                 ::predefined-avatar    1
                 ::custom-avatar-mode   false
                 ::location-suggestions {}})

(defn ->sub-db [data]
  (into {}
        (map (fn [[k v]] [(keyword "wh.logged-in.profile.db" (name k)) v]))
        data))
