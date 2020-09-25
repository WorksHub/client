(ns wh.common.spec
  (:require [clojure.spec.alpha :as s]))

(defmulti get-fields
  (fn [f] (if (keyword? f)
           :keyword
           (first f))))

(defmethod get-fields
  'clojure.spec.alpha/keys
  [f]
  (->> f
       (take-nth 2)
       (rest)
       (apply concat)
       (map (comp keyword name))
       (set)))

(defmethod get-fields
  'clojure.spec.alpha/nilable
  [f]
  (let [fs (rest f)]
    (apply concat (map get-fields fs))))

(defmethod get-fields
  'clojure.spec.alpha/merge
  [f]
  (let [fs (rest f)]
    (apply concat (map get-fields fs))))

(defmethod get-fields
  :keyword
  [f]
  (get-fields (s/form f)))

(defn spec->fields
  [s]
  (get-fields (s/form s)))
