(ns wh.common.time
  (:require
    #?(:cljs [cljsjs.moment])
    [#?(:cljs cljs-time.core
        :clj clj-time.core) :as t]
    [wh.util :as util]))

(defn human-time
  [t]
  #?(:cljs
     (.calendar (js/moment (str t) "YYYYMMDD'T'HHmmss"))
     :clj "Not implemented"))
