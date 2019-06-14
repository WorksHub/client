(ns wh.common.specs.video
  (:require
    [#?(:clj clojure.spec.alpha
        :cljs cljs.spec.alpha) :as s]
    [wh.common.specs.primitives :as p]))

(s/def :wh.video/youtube-id  ::p/non-empty-string)
(s/def :wh.video/thumbnail   ::p/non-empty-string)
(s/def :wh.video/description ::p/non-empty-string)

(s/def :wh/video (s/keys :req-un [:wh.video/youtube-id
                                  :wh.video/thumbnail]
                         :opt-un [:wh.video/description]))
