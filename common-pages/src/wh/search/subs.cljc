(ns wh.search.subs
  (:require [re-frame.core :refer [reg-sub]]
            [wh.db :as db]
            [wh.search.components :as components]))

(reg-sub
  ::search-results
  (fn [db _]
    (:wh.search/data db)))

(def empty-result {:empty true})

(reg-sub
  ::sections-with-results
  :<- [::search-results]
  (fn [results _]
    (->> components/sections-coll
         (map
           (fn [{:keys [id] :as tab}]
             (assoc tab :search-result
                    (get results id empty-result)))))))


(defn safe-sum [coll]
  (when (seq coll) (reduce + coll)))

(reg-sub
  ::results-count
  :<- [::sections-with-results]
  (fn [sections]
    (->> (map :search-result sections)
         (filter (complement :empty))
         (map :nbHits)
         ;; When there are no collections return nil, instead of 0.
         ;; We want to identify situation before data is present,
         ;; that's why we need safe-sum. To distinguish between
         ;; 0 results and nil
         (safe-sum))))

;; Used for server side render
(reg-sub
  ::query
  (fn [db _]
    (get-in db [:wh.db/page-params :query])))
