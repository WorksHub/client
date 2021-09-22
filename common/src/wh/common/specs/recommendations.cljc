(ns wh.common.specs.recommendations
  (:require [clojure.spec.alpha :as s]
            [wh.spec.user :as user-spec]))

(s/def :wh.recommendation/user-id ::user-spec/id)
(s/def :wh.recommendation/job-id :wh.job/id)
(s/def :wh.recommendation/score (s/double-in :min 0.0 :max 1.0 :NaN false :infinite? false))
(s/def :wh.recommendation/candidate-score :wh.recommendation/score)
(s/def :wh.recommendation/company-score :wh.recommendation/score)
(s/def :wh.recommendation/created-at :wh/date)

(s/def :wh.recommendation/job
  (s/keys :req-un [:wh.job/id
                   :wh.job/tags
                   :wh.job/location
                   :wh.job/remote]))

(s/def :wh.recommendation/scored-job
  (s/keys :req-un [:wh.job/id
                   :wh.job/tags
                   :wh.job/location
                   :wh.recommendation/candidate-score]
          :opt-un [:wh.recommendation/company-score]))

(s/def :wh.recommendation/user
  (s/keys :req-un [::user-spec/id
                   :wh.user/skills
                   ::user-spec/preferred-locations
                   ::user-spec/remote]))

(s/def :wh/recommendation
  (s/keys :req-un [:wh.recommendation/user-id
                   :wh.recommendation/job-id
                   :wh.recommendation/candidate-score
                   :wh.recommendation/created-at]
          :opt-un [:wh.recommendation/company-score]))

(s/def :wh.recommendation/entity-id (s/or :blog-id :wh.blog/id
                                          :job-id :wh.job/id))

(s/def :wh.recommendation/entity-type #{"blog" "user" "job"})

(s/def :wh.recommendation/item-to-be-recommended (s/or :job :wh/job
                                                       :blog :wh/blog))
