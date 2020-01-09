(ns wh.common.specs.primitives
  (:require [#?(:clj clojure.spec.alpha
                :cljs cljs.spec.alpha) :as s]
            [clojure.string :as str]
            [wh.common.url :as url]))

(defn valid-email? [email]
  (re-matches #"^[^@]+@[^@\\.]+[\\.].+" email))

(defn valid-url? [url]
  (or (str/starts-with? url "http://")
      (str/starts-with? url "https://")))

(defn valid-domain? [domain]
  (re-matches #"([a-z0-9-]+\.)+[a-z]+" domain))

(s/def ::email (s/and string? valid-email?))
(s/def ::url (s/and string? url/has-domain?))
(s/def ::domain (s/and string? valid-domain?))

(defn problematic-paths [spec val]
  (let [expl (s/explain-data spec val)]
    (map :path (::s/problems expl))))

(defn alphanumeric?
  [s]
  (not (re-find #"[^a-zA-Z0-9]" s)))

(defn alphanumeric-slug?
  [s]
  (not (re-find #"[^a-zA-Z0-9\-]" s)))

(s/def ::non-empty-string (s/and string? (complement str/blank?)))
(s/def ::non-empty-alphanumeric-string (s/and ::non-empty-string alphanumeric?))
(s/def ::non-empty-slug (s/and ::non-empty-string alphanumeric-slug?))

(s/def :http.path/params (s/nilable (s/map-of keyword? string?)))
(s/def :http/query-params (s/nilable (s/map-of (s/or :keyword keyword?
                                                     :string  string?)
                                               (s/or :string  string?
                                                     :strings (s/coll-of string?)))))

(s/def ::percentage (s/double-in :min 0 :max 100))
