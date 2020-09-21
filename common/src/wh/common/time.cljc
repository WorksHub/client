(ns wh.common.time
  (:require [#?(:cljs cljs-time.core
                :clj clj-time.core) :as t]
            [#?(:cljs cljs-time.format
                :clj clj-time.format) :as tf]
            [wh.common.text :as text]
            [wh.util :as util]))

(defn str->time
  [s formatter-kw]
  (when s
    (tf/parse (tf/formatters formatter-kw) s)))

(defn human-time
  [t]
  (let [secs (t/in-seconds (t/interval t (t/now)))]
    (cond
      ;; less than a minute
      (< secs 60)      (str secs " " (text/pluralize secs "second") " ago")
      ;; less than an hour
      (< secs 3600)    (let [s (int (/ secs 60))]
                         (str s " " (text/pluralize s "minute") " ago"))
      ;; less than an day
      (< secs 86400)   (let [s (int (/ secs 60 60))]
                         (str s " " (text/pluralize s "hour") " ago"))
      ;; less than 28 days
      (< secs 2419200) (let [s (int (/ secs 60 60 24))]
                         (str s " " (text/pluralize s "day") " ago"))
      ;; longer than 28 days
      :else            (tf/unparse (tf/formatter "d MMM, yyyy") t))))

(defn month+year
  [t]
  (when t
    (tf/unparse (tf/formatter "MMM yyyy") t)))
