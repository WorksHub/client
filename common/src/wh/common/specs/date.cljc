(ns wh.common.specs.date
  (:require
    [#?(:clj clj-time.coerce
        :cljs cljs-time.coerce) :as tc]
    [#?(:clj clj-time.core
        :cljs cljs-time.core) :as t]
    [#?(:clj clj-time.format
        :cljs cljs-time.format) :as tf]
    [#?(:clj clojure.spec.alpha
        :cljs cljs.spec.alpha) :as s]
    [#?(:clj clojure.spec.gen.alpha
        :cljs cljs.spec.gen.alpha) :as gen]
    [spec-tools.core :as st]))

(defn conform-date-time
  [s]
  (cond
    (inst? s) s
    (string? s)
    (try
      (tc/to-date (tf/parse (tf/formatters :date-time) s))
      (catch #?(:clj java.lang.IllegalArgumentException
                :cljs js/Error) e
        ::s/invalid))
    :else ::s/invalid))

(s/def :wh/date (st/spec
                 (s/with-gen
                   (s/conformer conform-date-time identity)
                   #(gen/return #?(:clj (java.util.Date.)
                                   :cljs (js/Date.)))) {:type 'String})) ;; TODO get custom scalars in Leona!
