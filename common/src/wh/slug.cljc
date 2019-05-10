(ns wh.slug
  (:require
    [bidi.bidi :as bidi]
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