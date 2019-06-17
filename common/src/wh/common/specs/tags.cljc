(ns wh.common.specs.tags
  (:require
    #?(:clj [wh.spec.common :as sc])
    [#?(:clj clojure.spec.alpha
        :cljs cljs.spec.alpha) :as s]
    [wh.common.specs.date]
    [wh.common.specs.primitives :as p]))

(s/def :wh.tag/type #{:tech})
(s/def :wh.tag/label ::p/non-empty-string)
(s/def :wh.tag/slug ::p/non-empty-slug)
(s/def :wh.tag/created-at :wh/date)
(s/def :wh.tag/id    #?(:clj  sc/string-uuid
                        :cljs string?))

(s/def :wh/tag (s/keys :req-un [:wh.tag/label
                                :wh.tag/id
                                :wh.tag/slug
                                :wh.tag/type]))
