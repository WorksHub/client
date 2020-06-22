(ns wh.landing-new.tags
  (:require [clojure.string :as str]))

(defn- parse-tags [tags-param]
  (->> (str/split tags-param #",")
       (map
         (fn [tag]
           (str/split tag #":")))
       (map (fn [[tag type]]
              {:slug tag :type type}))))


(defn param->tags [tags-param]
  (or (some-> tags-param parse-tags)
      []))
