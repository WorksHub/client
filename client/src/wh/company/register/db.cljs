(ns wh.company.register.db
  (:require [cljs.spec.alpha :as s]
            [wh.common.specs.location]
            [wh.common.specs.primitives :as primitives]
            [wh.db :as db]))

(def location-subs [:wh.company.register.subs/location-suggestions
                    :wh.company.register.events/select-location-suggestion])

(def company-subs [:wh.company.register.subs/company-suggestions
                   :wh.company.register.events/select-company-suggestion])

(def fields-properties [:label :key :initial :input :spec :suggestions])

(def company-fields
  [["* Name"    ::contact-name ""   :text     ::primitives/non-empty-string]
   ["* Company" ::company-name ""   :text     ::primitives/non-empty-string company-subs]
   ["* Email"   ::email        ""   :email    ::primitives/email]])

(def job-fields
  [["* Job title"       ::job-title        ""  :text     ::primitives/non-empty-string]
   ["* Location"        ::location         ""  :text     ::primitives/non-empty-string location-subs]
   ["* Skills"          ::tags             #{} :tags     ::selected-tags]
   ["* Job description" ::job-description  ""  :textarea ::primitives/non-empty-string]])

(def make-fields-map (partial map (partial zipmap fields-properties)))
(def make-empty-fields-map (partial into {} (map (juxt :key :initial))))

(defn valid-form?
  [m form]
  (every? #(s/valid? (:spec %) (get form (:key %))) m))

(def company-fields-maps (make-fields-map company-fields))
(def job-fields-maps (make-fields-map job-fields))

(def empty-company-fields (make-empty-fields-map company-fields-maps))
(def empty-job-fields (make-empty-fields-map job-fields-maps))

(defn location-details-valid?
  [db]
  (if-let [ld (::location-details db)]
    (s/valid? ::location-details ld)
    false))

(def valid-company-form? (partial valid-form? company-fields-maps))
(def valid-job-form? (partial valid-form? job-fields-maps))

;;;;;

(s/def ::step #{:company-details :job-details :complete})
(s/def ::loading? boolean?)
(s/def ::error keyword?)
(s/def ::tags-collapsed? boolean?)
(s/def ::tag ::primitives/non-empty-string)
(s/def ::tag-search string?)
(s/def ::selected? boolean?)
(s/def ::tag-container (s/keys :req-un [::tag]
                               :opt-un [::selected]))
(s/def ::available-tags (s/coll-of ::tag-container :distinct true))
(s/def ::selected-tags (s/coll-of ::tag-container :distinct true :kind set? :min-count 1))
(s/def ::company-id ::primitives/non-empty-string)

(s/def :clearbit/name ::primitives/non-empty-string)
(s/def :clearbit/domain ::primitives/non-empty-string)
(s/def :clearbit/logo ::primitives/non-empty-string)
(s/def ::company-suggestions (s/coll-of (s/keys :opt-un [:clearbit/name
                                                         :clearbit/domain
                                                         :clearbit/logo])))
(s/def ::description ::primitives/non-empty-string)
(s/def ::location-suggestions (s/coll-of (s/keys :req-un [::description])))
(s/def ::location-details (s/nilable :wh/location))
(s/def ::location-error? boolean?)
(s/def ::logo-uploading? boolean?)

(s/def ::sub-db (s/keys :req [::step
                              ::tags-collapsed?]
                        :opt [::checked-form
                              ::loading?
                              ::error
                              ::available-tags
                              ::tag-search
                              ::company-id
                              ::company-suggestions
                              ::location-suggestions
                              ::location-error?
                              ::logo-uploading?]))

(defn basic-db
  [db]
  (let [query-params (::db/query-params db)
        step (or (get-in db [:wh.db/page-params :step])
                 (get-in db [::sub-db ::step])
                 :company-details)]
    {::step    step
     ::tags-collapsed? true}))

(defn default-db
  [db]
  (merge empty-company-fields
         empty-job-fields
         (::sub-db db)
         (basic-db db)))
