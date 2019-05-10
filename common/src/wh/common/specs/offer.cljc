(ns wh.common.specs.offer
  (:require
   [#?(:clj clojure.spec.alpha
       :cljs cljs.spec.alpha) :as s]
   [#?(:clj clojure.spec.gen.alpha
       :cljs cljs.spec.gen.alpha) :as gen]
   [wh.common.specs.primitives :as p]))

(s/def :wh.offer/recurring-fee nat-int?)
(s/def :wh.offer/placement-percentage ::p/percentage)
(s/def :wh.offer/accepted-at inst?)
(s/def :wh/offer (s/keys :req-un [:wh.offer/recurring-fee
                                  :wh.offer/placement-percentage]
                         :opt-un [:wh.offer/accepted-at]))

(s/def :wh/pending-offer (s/keys :req-un [:wh.offer/recurring-fee
                                          :wh.offer/placement-percentage]))
