(ns wh.register.db
  (:require [cljs.spec.alpha :as s]
            [wh.util :refer [index-of]]))

(def step-order
  "The registration process consists of these steps, in the
  specified order."
  [:email :skills :location :name :verify :test :finish])

(s/def ::step (set step-order))
(s/def ::name string?)
(s/def ::available-skills (s/coll-of string?))
(s/def ::selected-skills (s/coll-of string? :kind set?))
(s/def ::add-skill-visible? boolean?)
(s/def ::new-skill string?)

(s/def ::manual-location? boolean?)
(s/def ::location-query string?)

(s/def ::city string?)
(s/def ::administrative string?)
(s/def ::country string?)
(s/def ::country-code (s/and string? #(= (count %) 2)))
(s/def ::sub-region (s/nilable string?))
(s/def ::region (s/nilable string?))
(s/def ::latitude (s/or :double (s/double-in :min -90.0 :max 90 :NaN false :infinite? false)
                        :int (s/int-in -90 90)))
(s/def ::longitude (s/or :double (s/double-in :min -180.0 :max 180 :NaN false :infinite? false)))

(s/def ::location (s/keys :req [::city ::country ::country-code]
                          :opt [::sub-region ::region
                                ::latitude ::longitude
                                ::administrative ::timezone]))
(s/def ::location-search-results (s/coll-of ::location))
(s/def ::location-search-error (s/nilable string?))
(s/def ::preferred-location (s/nilable ::location))
(s/def ::current-location (s/nilable ::location))

(s/def ::id string?)
(s/def ::email string?)
(s/def ::upsert-user-errors (s/nilable keyword?))

(s/def ::language string?)
(s/def ::riddle string?)
(s/def ::code-riddle (s/keys :req [::language ::riddle]))
(s/def ::code-riddles (s/coll-of ::code-riddle))
(s/def ::selected-riddle (s/nilable ::code-riddle))
(s/def ::code-answer string?)
(s/def ::failed-code-riddle-check? boolean?)
(s/def ::approval-fail? boolean?)
(s/def ::code-riddle-error boolean?)
(s/def ::skills-info-hidden? boolean?)
(s/def ::loading? boolean?)
(s/def ::consented (s/nilable string?))
(s/def ::subscribed? boolean?)
(s/def ::preset-name? boolean?)
(s/def ::remote boolean?)

(def location-stages-order [:confirm-preferred-location :ask-for-preferred-location :confirm-current-location :ask-for-current-location])
(s/def ::location-stage (set location-stages-order))

(s/def ::sub-db (s/keys :req [::step ::manual-location?
                              ::available-skills ::selected-skills
                              ::add-skill-visible?
                              ::failed-code-riddle-check?
                              ::skills-info-hidden? ::subscribed?
                              ::remote]
                        :opt [::name ::preset-name?
                              ::location-query ::location
                              ::location-search-results
                              ::new-skill
                              ::code-riddles ::selected-riddle
                              ::code-answer ::code-riddle-error ::email
                              ::id
                              ::upsert-user-errors
                              ::approval-fail?
                              ::loading? ::consented]))

(defn default-db [default-technologies]
  {::step (first step-order)
   ::available-skills default-technologies
   ::selected-skills #{}
   ::name ""
   ::manual-location? false
   ::add-skill-visible? false
   ::failed-code-riddle-check? false
   ::skills-info-hidden? false
   ::subscribed? false
   ::remote false})

(defn enum-compare
  [ordered-items item1 item2]
  (let [index1 (index-of ordered-items item1)
        index2 (index-of ordered-items item2)]
    (and index1 index2 (<= index1 index2))))

(def step<=
  "In the registration process, is step1 no later than step2?"
  (partial enum-compare step-order))

(def location-stage<=
  (partial enum-compare location-stages-order))

(defn effective-step
  "Returns the current registration step: either the one
   requested in the URL, or the one we've advanced to so far,
   whichever is earlier."
  [db]
  (let [url-step (get-in db [:wh.db/page-params :step])
        db-step (get-in db [::sub-db ::step])]
    (if (step<= url-step db-step)
      url-step
      db-step)))

(def next-step (zipmap step-order (next step-order)))
