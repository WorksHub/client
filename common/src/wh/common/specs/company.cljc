(ns wh.common.specs.company
  (:require
    [#?(:clj clojure.spec.alpha
        :cljs cljs.spec.alpha) :as s]
    [wh.common.specs.primitives :as p]))

(def sizes #{:micro :small :medium :large})

(def size->range
  {:micro  "1-9"
   :small  "10-49"
   :medium "50-249"
   :large  "250+"})

(s/def :wh.company/size sizes)

(s/def :wh.company/founded-year pos-int?)
(s/def :wh.company/how-we-work ::p/non-empty-string)
(s/def :wh.company/additional-tech-info ::p/non-empty-string)

(s/def :wh.company.tech-scales/testing        (s/double-in :min 0.0 :max 1.0))
(s/def :wh.company.tech-scales/ops            (s/double-in :min 0.0 :max 1.0))
(s/def :wh.company.tech-scales/time-to-deploy (s/double-in :min 0.0 :max 1.0))
(s/def :wh.company/tech-scales (s/keys :opt-un [:wh.company.tech-scales/testing
                                                :wh.company.tech-scales/ops
                                                :wh.company.tech-scales/time-to-deploy]))
