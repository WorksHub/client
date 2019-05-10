(ns wh.common.issue
  (:require
    #?(:clj [clj-time.format :as tf]
       :cljs [cljs-time.format :as tf])
    [clojure.string :as str]
    [wh.common.cases :as cases]
    [wh.util :as util]))

(defn parse-date-time
  [s]
  (tf/parse (tf/formatter "E MMM dd HH:mm:ss ZZZ yyyy")
            #?(:clj (str/replace s #"BST" "Etc/GMT+1") ;; joda time does not recognise BST, fml
               :cljs s)))

(defn gql-issue->issue
  [issue]
  (-> issue
      (cases/->kebab-case)
      (util/update-in* [:level] keyword)
      (util/update-in* [:status] keyword)
      (util/update-in* [:created-at] parse-date-time)))
