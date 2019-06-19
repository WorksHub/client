(ns wh.common.specs.company
  (:require
    [#?(:clj clojure.spec.alpha
        :cljs cljs.spec.alpha) :as s]))

(def sizes #{"micro" "small" "medium" "large"})

(s/def :wh.company/size sizes)
