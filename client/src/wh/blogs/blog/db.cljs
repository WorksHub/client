(ns wh.blogs.blog.db
  (:require
    [cljs.spec.alpha :as s]))

(s/def ::share-links-shown? boolean?)
(s/def ::author-info-visible? boolean?)
(s/def ::upvotes (s/map-of string? nat-int?))

(s/def ::sub-db (s/keys :req [::share-links-shown? ::author-info-visible? ::upvotes]))

(def default-db
  {::share-links-shown? false
   ::author-info-visible? false
   ::upvotes {}})
