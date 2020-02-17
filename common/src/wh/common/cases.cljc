(ns wh.common.cases
  (:require [camel-snake-kebab.core :as c]
            [camel-snake-kebab.extras :as ce]))

(defn ->kebab-case [m]
  (ce/transform-keys c/->kebab-case-keyword m))

(defn ->snake-case [m]
  (ce/transform-keys c/->snake_case_keyword m))

(defn ->camel-case [m]
  (ce/transform-keys c/->camelCaseKeyword m))

(defn ->camel-case-keys-str [m]
  (ce/transform-keys c/->camelCaseString m))

(def ->camel-case-str     c/->camelCaseString)
(def ->snake-case-str     c/->snake_case_string)
(def ->kebab-case-str     c/->kebab-case-string)
(def ->camel-case-keyword c/->camelCaseKeyword)
(def ->snake-case-keyword c/->snake_case_keyword)
(def ->kebab-case-keyword c/->kebab-case-keyword)
