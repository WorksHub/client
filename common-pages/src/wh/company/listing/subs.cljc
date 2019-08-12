(ns wh.company.listing.subs
  (:require
    [re-frame.core :refer [reg-sub reg-sub-raw]]
    [wh.re-frame.subs :refer [<sub]]
    [wh.common.location :as location]
    [wh.common.specs.company :as company-specs]
    [wh.company.profile.db :as profile])
  (#?(:clj :require :cljs :require-macros)
    [wh.re-frame.subs :refer [reaction]]))

(reg-sub ::db (fn [db _] db))

(defn page-number
  [qps]
  (let [p (get qps "page" "1")]
    #?(:cljs (js/parseInt p)
       :clj (Integer. p))))

(defn company-sort
  [qps]
  (get qps "sort" "created"))

(def tag-sort
  {:industry 1
   :funding  2
   :company  3
   :tech     4
   :benefit  5})

(def tag-limit
  {:industry 1
   :funding  1
   :company  2
   :tech     4
   :benefit  2})

(defn ->company
  [company]
  (let [tag-type-cull (->> (:tags company)
                           (sort-by :weight)
                           (reduce (fn [a tag]
                                     (let [tag-type (:type tag)
                                           new-count (inc (get-in a [:count tag-type] 0))]
                                       (-> a
                                           (assoc-in [:count tag-type] new-count)
                                           (update  :tags (if (<= new-count (tag-limit tag-type))
                                                            #(conj % tag)
                                                            identity))))) {:count {} :tags []}))
        tags   (->> (:tags tag-type-cull)
                    (map #(assoc % :score (tag-sort (:type %))))
                    (sort-by :score))
        location     (some-> company :locations first location/format-location)
        size         (company-specs/size->range (:size company))]
    (assoc company
           :tags     tags
           :location location
           :size     size)))

(reg-sub-raw
  ::companies
  (fn [_ _]
    (reaction
      (let [qps (<sub [:wh/query-params])
            {:keys [companies pagination] :as result}
            (:companies (<sub [:graphql/result :companies {:page_number (page-number qps)
                                                           :sort        (company-sort qps)}]))]
        {:pagination pagination
         :companies (map (fn [company]
                           (-> company
                               (profile/->company)
                               (->company))) companies)}))))

(reg-sub
  ::current-page
  :<- [:wh.subs/query-params]
  (fn [qps _]
    (page-number qps)))
