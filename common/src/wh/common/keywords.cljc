(ns wh.common.keywords
  (:require [camel-snake-kebab.core :as c]
            [clojure.walk :refer [postwalk]]))

(defn namespace-map
  "Namespace all keys in m with ns, unless they are already namespaced."
  [ns m]
  (let [ns (str ns)]
    (reduce-kv (fn [acc k v]
                 (let [k' (if (and (keyword? k)
                                   (not (namespace k)))
                            (keyword ns (name k))
                            k)]
                   (assoc acc k' v)))
               {} m)))

(defn strip-ns-from-map-keys
  "Strips all namespaces of a map's keys."
  [m]
  (into {} (map (fn [[k v]] [(keyword (name k)) v])) m))

(defn transform-keys
  "Recursively transforms all map keys in coll.
  It removes nil values (or in 2-arity version values that match the
  remove-value? predicate), strips ns from map keys and CamelCases keys"
  ([coll] (transform-keys nil? coll))
  ([remove-value? coll]
   (letfn [(transform [[k v]] [(c/->camelCaseKeyword k) v])]
     (postwalk (fn [x] (if (map? x)
                        (into {}
                              (comp (remove (fn [[_k v]] (remove-value? v)))
                                    (map (fn [[k v]] [(keyword (name k)) v]))
                                    (map transform))
                              x)
                        x)) coll))))
