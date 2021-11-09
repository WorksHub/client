(ns wh.common.spec
  (:require [clojure.spec.alpha :as s])
  (:import [clojure.lang MultiFn]))

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
    (mapcat get-fields fs)))

(defmethod get-fields
  'clojure.spec.alpha/merge
  [f]
  (let [fs (rest f)]
    (mapcat get-fields fs)))

(defmethod get-fields
  :keyword
  [f]
  (get-fields (s/form f)))

(defmethod get-fields
  'clojure.spec.alpha/or
  [f]
  (->> f
       (take-nth 2)
       (rest)
       (mapcat #(get-fields (s/form %)))))

(defmethod get-fields
  'clojure.spec.alpha/multi-spec
  [f]
  (let [^MultiFn multi-fn @(resolve (second f))
        method-table-keys (keys (.getMethodTable multi-fn))
        specs-for-methods (->> method-table-keys
                               (map #(.getMethod multi-fn %))
                               (map #(% :ignored-single-arg)))]
    (mapcat get-fields specs-for-methods)))

(defn spec->fields
  [s]
  (get-fields (s/form s)))
