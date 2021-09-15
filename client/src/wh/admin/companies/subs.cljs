(ns wh.admin.companies.subs
  (:require [clojure.string :as str]
            [re-frame.core :refer [reg-sub]]
            [wh.admin.companies.db :as companies]
            [wh.common.data :refer [get-manager-name]]))

(reg-sub
  ::sub-db
  (fn [db _] (::companies/sub-db db)))

(reg-sub
  ::loading-more?
  :<- [::sub-db]
  (fn [db _]
    (::companies/loading-more? db)))

(reg-sub
  ::show-load-more?
  :<- [::sub-db]
  :<- [::loading-more?]
  (fn [[db loading-more?] _]
    (and (::companies/show-load-more? db)
         (not loading-more?))))

(reg-sub
  ::tag-search
  :<- [::sub-db]
  (fn [db _]
    (::companies/tag-search db)))

(reg-sub
  ::tags
  :<- [::sub-db]
  (fn [db _]
    (::companies/tags db)))

(reg-sub
  ::tag-search-collapsed?
  :<- [::sub-db]
  (fn [db _]
    (::companies/tag-search-collapsed? db)))

(reg-sub
  ::search
  :<- [::sub-db]
  (fn [db _]
    (::companies/search db)))

(defn search-tag
  [s selected?]
  {:type :search :tag (str "search: \"" s "\"") :selected selected?})

(reg-sub
  ::matching-tags
  :<- [::search]
  :<- [::tag-search]
  :<- [::tags]
  (fn [[name-search search-term tags] _]
    (let [select-tags         (map #(if (contains? tags (:tag %))
                                      (assoc % :selected true)
                                      %)
                                   companies/tags)
          conflicting-types   (set (map :type (filter #(and (:selected %)
                                                            (= :live-jobs (:type %))) select-tags)))
          removed-conflicts   (remove #(and (not (:selected %))
                                            (contains? conflicting-types (:type %))) select-tags)
          filtered-tags       (filter #(or (:selected %)
                                           (str/blank? search-term)
                                           (str/includes? (str/lower-case (:tag %))
                                                          (str/lower-case search-term)))
                                      removed-conflicts)
          with-pending-search (if (str/blank? search-term)
                                filtered-tags
                                (conj filtered-tags (search-tag search-term false)))]
      (if (str/blank? name-search)
        with-pending-search
        (conj with-pending-search (search-tag name-search true))))))

(reg-sub
  ::results
  :<- [::sub-db]
  (fn [db _]
    (::companies/results db)))

(reg-sub
  ::sort
  :<- [::sub-db]
  (fn [db _]
    (get companies/sorts (::companies/sort db))))

(reg-sub
  ::sort-options
  :<- [::sub-db]
  (fn [db _]
    (vals companies/sorts)))

(reg-sub
  ::company-counts
  :<- [::sub-db]
  (fn [db _]
    [(count (::companies/results db)) (::companies/result-total db)]))
