(ns wh.common.specs.company
  (:require
    [#?(:clj clojure.spec.alpha
        :cljs cljs.spec.alpha) :as s]))

(def sizes #{:micro :small :medium :large})

(def size->range
  {:micro  "1-9"
   :small  "10-49"
   :medium "50-249"
   :large  "250+"})

(s/def :wh.company/size sizes)
