(ns wh.common.emoji
  (:require [clojure.string :as str]))

(defn int->char [i]
  #?(:clj (str (doto (StringBuilder.) (.appendCodePoint i)))
     :cljs (js/String.fromCodePoint i)))

(defn char->int [c]
  #?(:clj (int c)
     :cljs (.charCodeAt c 0)))

(defn country-code->emoji [country-code]
  (if (and country-code (<= 65 (char->int (first country-code)) 122))
    (->> country-code
         str/upper-case
         (map (comp int->char (partial + 127397) char->int))
         (str/join ""))
    country-code))
