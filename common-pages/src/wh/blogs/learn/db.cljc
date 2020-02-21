(ns wh.blogs.learn.db
  (:require
    [bidi.bidi :as bidi]
    [wh.common.text :as txt]
    [wh.util :as util]))

(def page-size 24)
(def search-query-name "blogs-search")

(defn tag [db]
  (bidi/url-decode (get-in db [:wh.db/page-params :tag])))

(defn current-page [db]
  (or (util/parse-int (get-in db [:wh.db/query-params "page"])) 1))

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
       :search_term     (search-term db)}
      util/remove-nils))
