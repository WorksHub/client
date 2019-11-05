(ns wh.common.specs.tags
  (:require
    #?(:clj [wh.spec.common :as sc])
    [#?(:clj clojure.spec.alpha
        :cljs cljs.spec.alpha) :as s]
    [spec-tools.core :as st]
    [wh.common.specs.date]
    [wh.common.specs.primitives :as p]))

(def types #{:tech :company :industry :funding :benefit})

;; Tech subtypes
;; `Software` - languages, frameworks + DBs,
;; `Ops` - ci/cd, source control, tests, tooling etc
;; `Infrastructure` - aws, heroku, saas solutions
;; `Tools` - hardware, IDEs
(def tech-subtypes #{:software :ops :infrastructure :tools})
(def benefit-subtypes #{:vacation :culture :health :finance :extra :professional_dev :diversity :parents})
(def all-subtypes (clojure.set/union tech-subtypes benefit-subtypes))

(s/def :wh.tag/type types)
(s/def :wh.tag.db/type (set (map name types)))
(s/def :wh.tag/label ::p/non-empty-string)
(s/def :wh.tag/slug ::p/non-empty-slug)
(s/def :wh.tag/created-at :wh/date)
(s/def :wh.tag/id    #?(:clj  sc/string-uuid
                        :cljs string?))
(s/def :wh.tag/subtype all-subtypes)
(s/def :wh.tag.db/subtype (set (map name all-subtypes)))

(s/def :wh.tag/weight (s/double-in :min 0 :max 1.0))

(s/def :wh/tag (s/keys :req-un [:wh.tag/id
                                :wh.tag/label
                                :wh.tag/slug
                                :wh.tag/weight
                                :wh.tag/type]
                       :opt-un [:wh.tag/subtype]))
(s/def :wh.tag/db-tag (s/keys :req-un [:wh.tag/id
                                       :wh.tag/label
                                       :wh.tag/slug
                                       :wh.tag/weight
                                       :wh.tag.db/type]
                              :opt-un [:wh.tag.db/subtype]))
(s/def :wh/tags (s/coll-of :wh/tag))

(s/def :wh.gql/tag (s/keys :opt-un [:wh.tag/id
                                    :wh.tag/label
                                    :wh.tag/slug
                                    :wh.tag/weight
                                    :wh.tag/type
                                    :wh.tag/subtype]))

(s/def :wh.gql/tags (s/coll-of :wh.gql/tag))

(s/def :wh/update-tag (s/keys :req-un [:wh.tag/id]
                              :opt-un [:wh.tag/label
                                       :wh.tag/slug
                                       :wh.tag/weight
                                       :wh.tag/type
                                       :wh.tag/subtype]))
