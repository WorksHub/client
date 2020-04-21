(ns wh.common.time
  (:require #?(:cljs ["moment" :as moment])
            [#?(:cljs cljs-time.core
                :clj clj-time.core) :as t]
            [wh.util :as util]))

(defn human-time
  [t]
  #?(:cljs
     (.calendar (moment (str t) "YYYYMMDD'T'HHmmss"))
     :clj "Not implemented"))
