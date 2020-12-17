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

(def leona-custom-scalar
  {:parse     #(tc/to-date (tf/parse (tf/formatters :date-time) %))
   :serialize #(cond (string? %) % ;; fingers crossed it's in the right format
                     (inst? %) (tf/unparse (tf/formatters :date-time) (tc/from-date %)))})

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

;; custom scalar provided in wh.graphql
(s/def :wh/date (st/spec
                  (s/with-gen
                    (s/conformer conform-date-time)
                    #(gen/return #?(:clj (java.util.Date.)
                                    :cljs (js/Date.))))
                  {:type '(custom :date)})) ;; required by leona
