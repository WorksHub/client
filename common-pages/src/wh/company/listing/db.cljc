(ns wh.company.listing.db
  (:require
    [clojure.string :as str]
    [wh.common.location :as location]
    [wh.common.specs.company :as company-specs]
    [wh.company.profile.db :as profile]
    [wh.components.pagination :as pagination]
    [wh.util :as util]))

(def page-size 20)
(def tag-field-id "companies-listing-tag-field")
(def search-query-name "companies-search")

(defn company-sort
  [qps]
  (get qps "sort" "popular"))

(defn company-size
  [qps]
  (get qps "size"))

(defn live-jobs-only
  [qps]
  (boolean (get qps "jobs")))

(defn search-term
  [qps]
  (get qps search-query-name))

(defn qps->tag-string
  [qps]
  (when-let [tag-or-tags (get qps "tag")]
    (str/join ";" (util/->vec tag-or-tags))))

(defn qps->query-body
  [qps]
  (util/remove-nils
    (merge {:page_number (pagination/qps->page-number qps)
            :page_size   page-size
            :sort        (company-sort qps)
            :search_term (search-term qps)}
           (when-let [tag-string (qps->tag-string qps)]
             {:tag_string tag-string})
           (when (live-jobs-only qps)
             {:live_jobs :some})
           (when-let [size (company-size qps)]
             {:size size}))))

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
  (let [company (profile/->company company)
        tag-type-cull (->> (:tags company)
                           (sort-by :weight >)
                           (reduce (fn [a tag]
                                     (let [tag-type  (:type tag)
                                           new-count (inc (get-in a [:count tag-type] 0))]
                                       (-> a
                                           (assoc-in [:count tag-type] new-count)
                                           (update  :tags (if (<= new-count (tag-limit tag-type))
                                                            #(conj % tag)
                                                            identity))))) {:count {} :tags []}))
        tags          (->> (:tags tag-type-cull)
                           (map #(assoc % :score (tag-sort (:type %))))
                           (sort-by :score >))
        location      (some-> company :locations first location/format-location)
        size          (company-specs/size->range (:size company))]
    (assoc company
           :tags     tags
           :location location
           :size     size)))
