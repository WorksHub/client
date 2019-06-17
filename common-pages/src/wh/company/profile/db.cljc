(ns wh.company.profile.db
  (:require [#?(:clj clojure.spec.alpha
                :cljs cljs.spec.alpha) :as s]))

(s/def ::photo-uploading? boolean?)
