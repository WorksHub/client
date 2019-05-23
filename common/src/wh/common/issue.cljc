(ns wh.common.issue
  (:require
    #?(:clj [clj-time.coerce :as tc]
       :cljs [cljs-time.coerce :as tc])
    #?(:clj [clj-time.format :as tf]
       :cljs [cljs-time.format :as tf])
    [clojure.string :as str]
    [wh.common.cases :as cases]
    [wh.util :as util]))

(def issue-sorting-fns
  {:title      :title
   :repository (comp #(str (:owner %) "/" (:name %)) :repo)
   :created-at (comp (partial * -1) tc/to-long (partial tf/parse (tf/formatters :date-time)) :created-at)})

(defn gql-issue->issue
  [issue]
  (-> issue
      (cases/->kebab-case)
      (util/update-in* [:level] keyword)
      (util/update-in* [:status] keyword)))
