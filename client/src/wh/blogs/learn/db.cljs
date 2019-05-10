(ns wh.blogs.learn.db
  (:require
    [bidi.bidi :as bidi]))

(def page-size 24)

(defn tag [db]
  (bidi/url-decode (get-in db [:wh.db/page-params :tag])))

(defn current-page [db]
  (int (or (get-in db [:wh.db/query-params "page"]) 1)))

(defn params [db]
  {:page_number (current-page db)
   :tag         (tag db)
   :vertical    (:wh.db/vertical db)})
