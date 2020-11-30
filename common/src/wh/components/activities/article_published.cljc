(ns wh.components.activities.article-published
  (:require [wh.components.activities.article :as article]
            [wh.components.activities.components :as components]))

(defn card [blog actor type opts]
  [components/card type
   [article/base-card blog actor type opts]])
