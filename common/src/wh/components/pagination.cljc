(ns wh.components.pagination
  (:require
    [wh.routes :as routes]
    [wh.util :as util]))

;; TODO merge with pagination.clj

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
  (if (zero? (rem total page-size))
    (quot total page-size)
    (inc (quot total page-size))))

(defn generate-pagination [current total]
  (when (and current total)
    (cond-> [1]
      (<= current 3) (concat (range 2 (if (< total 5) (inc total) 5)))
      (> current 3) (concat [:space] (range (- current 1) (if (< total (+ current 2)) (inc total) (+ current 2))))
      (and (> total 5) (> (- total current) 2)) (concat [:space])
      (and (> total 4) (>= (- total current) 2)) (concat [total]))))
