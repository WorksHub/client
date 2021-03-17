(ns wh.common.issue
  (:require
    #?(:clj [clj-time.coerce :as tc]
       :cljs [cljs-time.coerce :as tc])
    #?(:clj [clj-time.format :as tf]
       :cljs [cljs-time.format :as tf])
    [clojure.string :as str]
    [wh.common.cases :as cases]
    [wh.util :as util]))

(defn gql-issue->issue
  [issue]
  (-> issue
      (cases/->kebab-case)
      (util/update* :level keyword)
      (util/update* :status keyword)))

(defn format-compensation [{:keys [compensation] :as _issue}]
  (if (and compensation (:currency compensation) (pos? (:amount compensation)))
    (str (name (:currency compensation)) " " (:amount compensation))))
