(ns wh.common.attachments
  (:require [clojure.string :as str]))

(defn downloadable? [filename]
  (some-> filename 
          (str/ends-with? ".pdf") 
          not))

