(ns wh.components.pagination
  (:require [wh.common.numbers :as numbers]
            [wh.routes :as routes]
            [wh.util :as util]))

;; TODO merge with pagination.clj

(defn qps->page-number
  [qps]
  (-> qps
      (get "page" "1")
      (numbers/parse-int)))

(defn pagination [current-page-number page-numbers route query-params & [params]]
  [:nav.pagination.is-centered {:role "pagination" :aria-label "pagination"}
   [:ul.pagination-list
    (when-not (<= (count page-numbers) 1)
      (map-indexed
        (fn [i page-number]
          (if (= :space page-number)
            [:li {:key (str page-number i)} [:span.pagination-ellipsis "\u22EF"]]
            [:li {:key page-number}
             [:a {:aria-label (str "Go to page " page-number)
                  :href       (routes/path route
                                           :params params
                                           :query-params (assoc query-params "page" page-number))
                  :class      (util/merge-classes "pagination-link"
                                                  (when (= page-number current-page-number) "is-current"))}
              page-number]]))
        page-numbers))]])

(defn number-of-pages [page-size total]
  (if (and total page-size)
    (if (zero? (rem total page-size))
      (quot total page-size)
      (inc (quot total page-size)))
    0))

(defn generate-pagination [current total]
  (when (and current total)
    (cond-> [1]
            (<= current 3) (concat (range 2 (if (< total 5) (inc total) 5)))
            (> current 3) (concat [:space] (range (- current 1) (if (< total (+ current 2)) (inc total) (+ current 2))))
            (and (> total 5) (> (- total current) 2)) (concat [:space])
            (and (> total 4) (>= (- total current) 2)) (concat [total]))))

(defn results-label
  [element-plural total-count current-page-number page-size]
  (when (and total-count page-size current-page-number)
    (let [start (inc (* page-size (dec current-page-number)))
          end (min (* page-size current-page-number) total-count)]
      (cond
        (zero? total-count)
        "No results match your search"
        (< total-count page-size)
        (str "Showing " 1 "-" total-count " of " total-count " " element-plural)
        :else
        (str "Showing " start "-" end " of " total-count " " element-plural)))))
