(ns wh.re-frame.subs
  (:require
    [re-frame.core :as re-frame]))

(def <sub (comp deref re-frame/subscribe))
