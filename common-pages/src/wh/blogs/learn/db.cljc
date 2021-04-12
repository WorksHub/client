(ns wh.blogs.learn.db
  (:require [bidi.bidi :as bidi]
            [wh.common.numbers :as numbers]
            [wh.common.text :as txt]
            [wh.util :as util]))

(def page-size 24)
(def search-query-name "blogs-search")

(defn tag [db]
  (bidi/url-decode (get-in db [:wh.db/page-params :tag])))

(defn current-page [db]
  (or (numbers/parse-int (get-in db [:wh.db/query-params "page"])) 1))

(defn search-term [db]
  (txt/not-blank
    (get-in db [:wh.db/query-params search-query-name])))

(defn params [db]
  (-> {:page_number     (current-page db)
       :page_size       24
       :tag             (tag db)
       :vertical        (:wh.db/vertical db)
       ;; we want to fetch blogs from all verticals if tag is selected
       :vertical_blogs  (when-not (tag db) (:wh.db/vertical db))
       :promoted_amount 8
       :issues_amount   8
       :search_term     (or (search-term db) (tag db))}
      util/remove-nils))

(defn search-arguments? [db]
  (or (not (empty? (search-term db)))
      (not (empty? (tag db)))))

(defn articles-jobs-params [db]
  (cond-> (params db)
    (search-arguments? db)
    ;; we want only 3 recommended jobs
    (assoc :page_size 3)))

(defn articles-issues-params [db]
  (cond-> (params db)
    (tag db) (assoc :repo_language (tag db))
    :always (assoc :issues_amount 3)))
