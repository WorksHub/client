(ns wh.search.subs
  (:require [re-frame.core :refer [reg-sub]]
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
    (map
      (fn [{:keys [id] :as tab}]
        (assoc tab :search-result
               (get results id empty-result)))
      components/sections-coll)))


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

(reg-sub
  ::search-results-tags
  :<- [::search-results]
  (fn [results _]
    (->> results
         vals
         (mapcat :hits)
         (mapcat :tags)
         ;; leave only technology-related tags
         (filter #(#{"tech"} (:type %)))
         ;; leave only distinct (by `objectID`)
         (group-by :objectID)
         (map (comp first val))
         ;; change algoia ids into tag ids
         (map #(assoc % :id (:objectID %)))
         ;; too many tags would pollute UI. we want users to click
         ;; tags, not to spend half an hour scrolling through them
         (take components/max-displayed-tags))))
