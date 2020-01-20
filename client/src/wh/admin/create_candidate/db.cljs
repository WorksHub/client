(ns wh.admin.create-candidate.db
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.db :refer [app-db]]
            [wh.common.data :as data]
            [wh.common.specs.location]
            [wh.common.specs.primitives :as p]
            [wh.components.forms.db :as forms]
            [wh.db :as db]
            [wh.user.db :as user]))

(s/def ::tags (s/coll-of any? :min-count 1))

(def fields
  {::name                {:order 0 :initial "", :validate ::p/non-empty-string}
   ::email               {:order 1 :initial "", :validate ::p/email}
   ::phone               {:order 2 :initial ""}
    ::notify              {:order 3 :initial true}

   ::location-search       {:order 30 :initial "" :event? false}

   ::current-company        {:order 40, :initial nil}
   ::current-company-search {:order 41, :initial ""}
   ::resume-url             {:order 50, :initial nil}
   ::github-url             {:order 60, :initial ""}
   ::other-links            {:order 61, :initial [], :sub? false, :event? false}
   ::tech-tag-search        {:order 70, :initial ""}
   ::tech-tags              {:order 71, :initial #{}, :validate ::tags}
   ::company-tag-search     {:order 80, :initial ""}
   ::company-tags           {:order 81, :initial #{}}})

;; this is hacky, but all of this calls for a refactor
(defmulti field-has-error? (fn [db k] k))

(defmethod field-has-error? ::location-search
  [db k]
  ;; FIXME: this should not be necessary; after all, when we get here,
  ;; wh.location/* specs are defined because we've required
  ;; wh.common.specs.location explicitly, right? Wrong. It gets loaded
  ;; later on. Looks like a compiler bug to me...
  (when (s/get-spec :wh.location/country-code)
    (when-not (and (s/valid? :wh.location/city (::location__city db))
                   (s/valid? :wh.location/country-code (::location__country-code db)))
      "You must pick a location with city and country.")))

(defmethod field-has-error? :default
  [db k]
  (when-let [spec (get-in fields [k :validate])]
    (let [value (get db k)]
      (not (s/valid? spec value)))))

(defn invalid-fields
  [sub-db]
  (when-let [keys (seq (filter (partial field-has-error? sub-db) (keys fields)))]
    (->> keys
         (map #(vector % (get-in fields [% :order])))
         (sort-by second)
         (map first))))

(defn initial-db [db]
  (forms/initial-value fields))
