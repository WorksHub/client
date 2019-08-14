(ns wh.company.register.subs
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [re-frame.core :refer [reg-sub]]
    [wh.common.errors :as errors]
    [wh.common.specs.primitives :as primitives]
    [wh.company.register.db :as register]
    [wh.subs :refer [error-sub-key]]))

(doseq [{k :key} register/company-fields-maps]
  (reg-sub
    k
    (fn [db _]
      (get-in db [::register/sub-db k]))))

(def error-msgs {::primitives/non-empty-string "This field can't be empty."
                 ::primitives/email "This is not a valid email."
                 ::register/selected-tags "One or more tags required."})

(defn get-server-error-string
  [k]
  (case k
    :duplicate-user "A user with this email address is already registered in our system. Please use a unique email address."
    :unknown-error "An unknown error or timeout has occurred. Please check your connection and try again."
    (errors/upsert-user-error-message k)))

(defn error-query
  [db k spec]
  (let [value (get-in db [::register/sub-db k])]
    (and (not (s/valid? spec value))
         (= value (get-in db [::register/sub-db ::register/checked-form k]))
         (get error-msgs spec "no error msg specified"))))

(doseq [{k :key spec :spec} register/company-fields-maps]
  (reg-sub
    (error-sub-key k)
    (fn [db _]
      (error-query db k spec))))

(reg-sub
  ::company-signup-form-checked?
  (fn [db _]
    (= (set (map :key register/company-fields-maps))
       (set (keys (get-in db [::register/sub-db ::register/checked-form]))))))

(reg-sub
  ::loading?
  (fn [db _]
    (get-in db [::register/sub-db ::register/loading?])))

(reg-sub
  ::logo-uploading?
  (fn [db _]
    (get-in db [::register/sub-db ::register/logo-uploading?])))

(reg-sub
  ::error
  (fn [db _]
    (or (some-> db
                (get-in [::register/sub-db ::register/error])
                (get-server-error-string))
        (when (get-in db [::register/sub-db ::register/location-error?])
          "The specified location was not recognised. Please try again."))))

(defn format-suggestions
  [db input suggestions map-fn]
  (let [original (some-> db (get-in [::register/sub-db input]) (str/lower-case))]
    (when-not (str/blank? original)
      (let [results (->> (get-in db [::register/sub-db suggestions])
                         (map map-fn)
                         (map #(hash-map :id % :label %))
                         (take 5)
                         (vec))]
        (if (and (= 1 (count results))
                 (= original (some-> results (first) (:label) (str/lower-case))))
          (vector)
          results)))))

(reg-sub
  ::company-suggestions
  (fn [db _]
    (format-suggestions db ::register/company-name ::register/company-suggestions :name)))