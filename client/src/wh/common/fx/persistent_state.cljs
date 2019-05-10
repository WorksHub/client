(ns wh.common.fx.persistent-state
  (:require
    [alandipert.storage-atom :as storage]
    [cognitect.transit :as transit]
    [re-frame.core :refer [reg-cofx reg-fx]])
  (:import
    [goog.date UtcDateTime]))

;; Enable cljs-time values to be serialized
;; Adapted from https://github.com/andrewmcveigh/cljs-time/issues/15#issuecomment-173706115

(def transit-readers
  {"m" (transit/read-handler (fn [s] (UtcDateTime.fromTimestamp s)))})

(def transit-writers
  {UtcDateTime (transit/write-handler
                (constantly "m")
                (fn [v] (.getTime v))
                (fn [v] (str (.getTime v))))})

(swap! storage/transit-read-handlers merge transit-readers)
(swap! storage/transit-write-handlers merge transit-writers)

(def persistent-state
  (storage/local-storage (atom {}) :persistent-state))

(reg-fx
  :persist-state
  (fn [value]
    (reset! persistent-state value)))

(reg-cofx
  :persistent-state
  (fn [coeffects _]
    (assoc coeffects :persistent-state @persistent-state)))
