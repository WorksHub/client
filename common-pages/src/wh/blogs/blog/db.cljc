(ns wh.blogs.blog.db
  (:require [#?(:cljs cljs.spec.alpha :clj clojure.spec.alpha) :as s]))

(defn id
  [db]
  (get-in db [:wh.db/page-params :id]))

(s/def ::share-links-shown? boolean?)
(s/def ::author-info-visible? boolean?)
(s/def ::upvotes (s/map-of string? nat-int?))

(s/def ::sub-db (s/keys :req [::share-links-shown? ::author-info-visible? ::upvotes]))

(def default-db
  {::share-links-shown? true
   ::author-info-visible? false
   ::upvotes {}})

(def page-size 9)

(defn params [db]
  {:id        (id db)
   :page_size page-size
   :vertical  (:wh.db/vertical db)})
