(ns wh.slug
  (:require [bidi.bidi :as bidi]
            [clojure.string :as str]))

(defn slug [s]
  "We currently have this function and wh.url/slugify (clojure only).
   We should have only one, consistent and cross-platform way of slugging, see story [ch2575]."
  (-> s
      (str/lower-case)
      (str/trim)
      (str/replace #"\s+" "-")))

(defn slug+encode [s]
  (bidi/url-encode (slug s)))

(defn tag-label->slug [s]
  (-> s
      (str/lower-case)
      (str/trim)
      (str/replace #"#" "sharp")  ;; C#
      (str/replace #"\+" "plus")  ;; C++
      (str/replace #"^\." "dot")  ;; .NET
      (str/replace #"[^a-zA-Z0-9]+" "-")))

(defn slug->label
  "Changes URL-safe string into readable label. Useful when URL parameter
  is passed to DB query."
  [s]
  (-> s
      (str/lower-case)
      (str/trim)
      (str/replace #"sharp" "#")  ;; C#
      (str/replace #"plus" "+")   ;; C++
      (str/replace #"dot" ".")))  ;; .NET
