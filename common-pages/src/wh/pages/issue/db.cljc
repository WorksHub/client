(ns wh.pages.issue.db
  (:require
    #?(:clj [clojure.spec.alpha :as s]
       :cljs [cljs.spec.alpha :as s])))

(def num-other-issues-to-show 2)
(def num-related-jobs-to-show 2)

(defn id
  [db]
  (get-in db [:wh.db/page-params :id]))

(s/def ::show-cta-sticky? boolean?)

(defn default-db
  [db]
  (merge db
         {::show-cta-sticky? false}))

(defn repo->full-name [repo]
  (str (:owner repo) "/" (:name repo)))

(defn page-title [{:keys [repo title]}]
  (str (repo->full-name repo) " Issue - " title))
