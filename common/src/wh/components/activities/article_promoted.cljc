(ns wh.components.activities.article-promoted
  (:require [wh.components.activities.article :as article]
            [wh.components.activities.components :as components]
            [wh.styles.activities :as styles]))

(defn card [blog actor type opts]
  [components/card type
   [components/promoter actor]

   [components/quoted-description {:class styles/quoted-description--article} ""]

   [article/base-card blog actor type opts]])
