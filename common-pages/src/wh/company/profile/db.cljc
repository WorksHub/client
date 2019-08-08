(ns wh.company.profile.db
  (:require
    [#?(:clj  clojure.spec.gen.alpha
        :cljs cljs.spec.gen.alpha) :as gen]
    [#?(:clj clojure.spec.alpha
        :cljs cljs.spec.alpha) :as s]
    [wh.common.specs.company]
    [wh.common.specs.location]
    [wh.util :as util]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; some details for the create profile view

(defn field->section
  [f]
  (case f
    :logo         1
    :name         1
    :description  2
    :industry-tag 3
    :funding-tag  3
    :size         3
    :founded-year 3
    :tech-tags    4
    :benefit-tags 5
    ;; else
    99))

(defn section->id
  [n]
  (str "company-profile__create-profile__section-" n))

(s/def ::description  :wh.company.profile/description-html)
(s/def ::industry-tag (s/and :wh/tag #(= :industry (:type %))))
(s/def ::funding-tag  (s/and :wh/tag #(= :funding  (:type %))))
(s/def ::tech-tags    (s/coll-of (s/and :wh/tag #(= :tech (:type %))) :min-count 1))
(s/def ::benefit-tags (s/coll-of (s/and :wh/tag #(= :benefit (:type %))) :min-count 1))

(s/def ::form (s/keys :req-un [:wh.company.profile/logo
                               :wh.company/name
                               ::description
                               ::industry-tag
                               ::funding-tag
                               ::tech-tags
                               ::benefit-tags
                               :wh.company/size
                               :wh.company/founded-year]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::photo-uploading? boolean?)
(s/def ::updating? boolean?)
(s/def ::creating-tag? boolean?)
(s/def ::video-error (s/nilable keyword?))

(s/def ::tag-search (s/map-of :wh.tag/type string?))
(s/def ::selected-tag-ids (s/map-of :wh.tag/type (s/coll-of :wh.tag/id)))

(s/def ::location-search string?)
(s/def ::location-suggestions (s/coll-of any?))
(s/def ::pending-location :wh/location)

(s/def ::logo-uploading? boolean?)
(s/def ::pending-logo string?)

(s/def ::publishing? boolean?)
(s/def ::show-sticky? boolean?)

(s/def ::error-map (s/with-gen
                     (s/map-of keyword? fn?)
                     #(gen/return {:test identity})))

(defn ->tag
  [m]
  (-> m
      (update :type keyword)
      (util/update-in* [:subtype] keyword)))

(defn ->company
  [m]
  (-> m
      (update :tags (partial map ->tag))
      (update :size keyword)))
