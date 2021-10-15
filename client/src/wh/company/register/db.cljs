(ns wh.company.register.db
  (:require
    [cljs.spec.alpha :as s]
    [wh.common.specs.primitives :as primitives]))

(def company-name-config [:wh.company.register.subs/company-suggestions
                          :wh.company.register.events/select-company-suggestion])

(def source-config [:wh.company.register.subs/source-suggestions
                    :wh.company.register.events/select-source-suggestion])

(def fields-properties [:label :key :initial :input :spec :suggestions])

(def company-fields
  [["* Your Name"                  ::contact-name ""   :text     ::primitives/non-empty-string]
   ["* Company Name"               ::company-name ""   :text     ::primitives/non-empty-string company-name-config]
   ["* Your Email"                 ::email        ""   :email    ::primitives/email]
   ["How did you hear about us?"   ::source       ""   :select   string? source-config]])

(def make-fields-map (partial map (partial zipmap fields-properties)))
(def make-empty-fields-map (partial into {} (map (juxt :key :initial))))

(defn valid-form?
  [m form]
  (every? #(s/valid? (:spec %) (get form (:key %))) m))

(def company-fields-maps (make-fields-map company-fields))
(def empty-company-fields (make-empty-fields-map company-fields-maps))
(def valid-company-form? (partial valid-form? company-fields-maps))

;;;;;
(s/def ::loading? boolean?)
(s/def ::error keyword?)
(s/def ::selected? boolean?)
(s/def ::company-id ::primitives/non-empty-string)

(s/def :clearbit/name ::primitives/non-empty-string)
(s/def :clearbit/domain ::primitives/non-empty-string)
(s/def :clearbit/logo ::primitives/non-empty-string)
(s/def ::company-suggestions (s/coll-of (s/keys :opt-un [:clearbit/name
                                                         :clearbit/domain
                                                         :clearbit/logo])))
(s/def ::description ::primitives/non-empty-string)
(s/def ::logo-uploading? boolean?)
(s/def ::other-mode? boolean?)

(s/def ::sub-db (s/keys :opt [::checked-form
                              ::loading?
                              ::error
                              ::company-id
                              ::company-suggestions
                              ::logo-uploading?
                              ::other-mode?]))

(defn default-db
  [db]
  (merge empty-company-fields
         (::sub-db db)))
