(ns wh.graphql-macros
  (:require
    [clojure.string :as str]
    [clojure.walk :refer [prewalk]]
    [venia.core :as venia]))

(def fragments (atom {}))
(def templates (atom {}))

(defn- find-fragments [query]
  (->> query
       (tree-seq coll? seq)
       (filter #(and (keyword? %) (= (namespace %) "fragment")))
       distinct
       (map name)
       (select-keys @fragments)
       vals))

(defmacro deffragment
  "Define a set of fields reusable across precompiled queries as a fragment."
  [fragment type fields]
  (let [fragment-name (name fragment)]
    (swap! fragments assoc fragment-name {:fragment/name fragment-name, :fragment/type type, :fragment/fields fields}))
  nil)

(defmacro defquery [name query]
  (let [fragments (find-fragments query)
        query (cond-> (eval query) fragments (assoc :venia/fragments (vec fragments)))]
    `(def ~name ~(venia/graphql-query query))))

(defmacro def-query-template [name template]
  (swap! templates assoc name template)
  nil)

(defn- expand-template
  [template-name substitutions]
  (let [definition (@templates template-name)]
    (prewalk (fn [node]
               (if (and (symbol? node) (str/starts-with? (name node) "$"))
                 (get substitutions (keyword (subs (name node) 1)) node)
                 node))
             definition)))

(defmacro def-query-from-template
  [name template-name substitutions]
  `(defquery ~name ~(expand-template template-name substitutions)))
