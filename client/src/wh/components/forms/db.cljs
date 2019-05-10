(ns wh.components.forms.db
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]))

(defn initial-value [fields]
  (zipmap (keys fields) (map :initial (vals fields))))

(defn field-valid? [fields db field]
  (if-let [spec (get-in fields [field :validate])]
    (s/valid? spec (get db field))
    true))

(defn form-valid? [fields db]
  (every? (partial field-valid? fields db) (keys fields)))

(defmulti invalid-field-message identity)

(defmethod invalid-field-message :default [field]
  (str (str/capitalize (name field)) " is invalid."))

(defn validate-field [fields db field]
  (when-let [spec (get-in fields [field :validate])]
    (when-not (s/valid? spec (get db field))
      (invalid-field-message field))))

(defn validate-form [fields db]
  (->> (mapv (partial validate-field fields db) (keys fields))
       (remove nil?)))

(defn invalid-fields
  [fields db]
  (->> fields
       (keys)
       (keep #(when-not (field-valid? fields db %) %))
       (not-empty)))
