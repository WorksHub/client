;; This is placeholder code. Do not add config here.

(ns wh.config
  (:refer-clojure :exclude [get get-in]))

(def config {})

(defn get-in
  [ks]
  (clojure.core/get-in config ks))

(defn get
  [k]
  (clojure.core/get config k))
