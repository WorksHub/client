(ns wh.interceptors
  (:require
    [re-frame.core :refer [trim-v]]))

(def default-interceptors [trim-v])
