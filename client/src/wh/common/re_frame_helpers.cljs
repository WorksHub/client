(ns wh.common.re-frame-helpers
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch]]))

(defn merge-classes
  [& classes]
  (str/join " " (remove nil? classes)))

(defn dispatch-on-enter [re-event]
  (fn [event]
    (when (= (.-key event) "Enter")
      (dispatch re-event))))
