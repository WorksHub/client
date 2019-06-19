(ns wh.common.specs.tags
  (:require
    #?(:clj [wh.spec.common :as sc])
    [#?(:clj clojure.spec.alpha
        :cljs cljs.spec.alpha) :as s]
    [wh.common.specs.date]
    [wh.common.specs.primitives :as p]))

(def types #{:tech :company :industry :funding})
(s/def :wh.tag/type types)
(s/def :wh.tag.db/type (set (map name types)))
(s/def :wh.tag/label ::p/non-empty-string)
(s/def :wh.tag/slug ::p/non-empty-slug)
(s/def :wh.tag/created-at :wh/date)
(s/def :wh.tag/id    #?(:clj  sc/string-uuid
                        :cljs string?))

(s/def :wh/tag (s/keys :req-un [:wh.tag/id
                                :wh.tag/label
                                :wh.tag/slug
                                :wh.tag/type]))
(s/def :wh.tag/db-tag (s/keys :req-un [:wh.tag/id
                                       :wh.tag/label
                                       :wh.tag/slug
                                       :wh.tag.db/type]))
(s/def :wh/tags (s/coll-of :wh/tag))
