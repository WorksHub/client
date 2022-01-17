(ns wh.admin.companies.db
  (:require
    [clojure.string :as str]
    [wh.common.data :as data]
    [wh.db :as db]
    [wh.verticals :as verticals]))

(def tags
  (concat (map #(hash-map :type :vertical :tag (str % " works")) verticals/available-job-verticals)
          (map #(hash-map :type :manager  :tag %) (vals data/managers))
          (map #(hash-map :type :package  :tag %) (map (fn [p] (str/replace (name p) #"_" " ")) data/packages))
          [{:type :manager   :tag "no manager"}]
          [{:type :has-users :tag "has users"}]
          [{:type :live-jobs :tag "with live jobs"}]
          [{:type :live-jobs :tag "without live jobs"}]))

(def sorts
  {:alpha   "Alphabetical"
   :created "Created Date"
   :updated "Updated Date"
   :pending "Pending Applications"})

(defn initialize-db
  [db]
  {::tag-search-collapsed? true
   ::page-number           1
   ::search                (get-in db [::db/query-params "search"])
   ::sort                  (keyword (get-in db [::db/query-params "sort"] "created"))
   ::tags                  (some->
                             (get-in db [::db/query-params "tags"])
                             (str/split #",")
                             (set))})
